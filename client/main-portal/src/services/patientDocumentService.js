const API_BASE_URL = (
	import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
).replace(/\/$/, '')

function authHeaders(token) {
	return {
		Authorization: `Bearer ${token}`,
	}
}

async function request(path, options = {}) {
	const { headers: customHeaders, ...restOptions } = options

	const response = await fetch(`${API_BASE_URL}${path}`, {
		credentials: 'include',
		...restOptions,
		headers: {
			'Content-Type': 'application/json',
			...(customHeaders || {}),
		},
	})

	const contentType = response.headers.get('content-type') || ''
	const payload = contentType.includes('application/json')
		? await response.json()
		: await response.text()

	if (!response.ok) {
		const message =
			typeof payload === 'string'
				? payload
				: payload?.message || payload?.error || 'Request failed'
		const error = new Error(message)
		error.status = response.status
		error.payload = payload
		throw error
	}

	return payload
}

function buildUploadInitQuery({ fileName, contentType, documentType, documentSubType, displayName }) {
	const params = new URLSearchParams()

	if (!fileName) throw new Error('fileName is required')
	if (!contentType) throw new Error('contentType is required')

	params.set('fileName', fileName)
	params.set('contentType', contentType)

	if (documentType) params.set('documentType', documentType)
	if (documentSubType) params.set('documentSubType', documentSubType)
	if (displayName) params.set('displayName', displayName)

	return params.toString()
}

/**
 * Step 1: get a presigned upload URL from patient-service.
 * Response shape:
 * { uploadUrl: string, permanentUrl: string | null }
 */
export async function initializeDocumentUpload({
	token,
	fileName,
	contentType,
	documentType,
	documentSubType,
	displayName,
}) {
	if (!token) throw new Error('token is required')

	const query = buildUploadInitQuery({
		fileName,
		contentType,
		documentType,
		documentSubType,
		displayName,
	})

	return request(`/patients/me/documents/upload-url?${query}`, {
		method: 'POST',
		headers: authHeaders(token),
	})
}

/**
 * Step 2: upload binary directly to the presigned URL.
 * No Authorization header required.
 */
export async function uploadDocumentToPresignedUrl({ uploadUrl, file, contentType }) {
	if (!uploadUrl) throw new Error('uploadUrl is required')
	if (!file) throw new Error('file is required')

	const response = await fetch(uploadUrl, {
		method: 'PUT',
		headers: {
			'Content-Type': contentType || file.type || 'application/octet-stream',
		},
		body: file,
	})

	if (!response.ok) {
		const text = await response.text().catch(() => '')
		const error = new Error(text || 'Upload failed')
		error.status = response.status
		throw error
	}

	return {
		ok: true,
		status: response.status,
	}
}

/**
 * Optional fallback endpoint (normally not needed in async event flow).
 */
export async function saveDocumentMetadataFallback({ token, payload }) {
	if (!token) throw new Error('token is required')

	return request('/patients/me/documents', {
		method: 'POST',
		headers: authHeaders(token),
		body: JSON.stringify(payload || {}),
	})
}

export async function getMyDocuments({ token, documentType }) {
	if (!token) throw new Error('token is required')

	const query = documentType
		? `?documentType=${encodeURIComponent(documentType)}`
		: ''

	return request(`/patients/me/documents${query}`, {
		headers: authHeaders(token),
	})
}

export async function getMyDocumentDownloadUrl({ token, documentId }) {
	if (!token) throw new Error('token is required')
	if (!documentId) throw new Error('documentId is required')

	return request(`/patients/me/documents/${documentId}/download-url`, {
		headers: authHeaders(token),
	})
}

export async function deleteMyDocument({ token, documentId }) {
	if (!token) throw new Error('token is required')
	if (!documentId) throw new Error('documentId is required')

	return request(`/patients/me/documents/${documentId}`, {
		method: 'DELETE',
		headers: authHeaders(token),
	})
}

export default {
	initializeDocumentUpload,
	uploadDocumentToPresignedUrl,
	saveDocumentMetadataFallback,
	getMyDocuments,
	getMyDocumentDownloadUrl,
	deleteMyDocument,
}
