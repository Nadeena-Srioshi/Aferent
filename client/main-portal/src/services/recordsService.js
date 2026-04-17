import patientDocumentService from '@/services/patientDocumentService'
import { getDoctorById } from '@/services/doctorService'

const API_BASE_URL = (
	import.meta.env.VITE_API_BASE_URL ||
	(window.location.hostname === 'localhost' ? 'http://localhost:8080' : '')
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
		const message = typeof payload === 'string'
			? payload
			: payload?.message || payload?.error || 'Request failed'
		const error = new Error(message)
		error.status = response.status
		error.payload = payload
		throw error
	}

	return payload
}

export async function getMyDocuments({ token, documentType }) {
	return patientDocumentService.getMyDocuments({ token, documentType })
}

export async function initializeDocumentUpload({ token, fileName, contentType, documentType, documentSubType, displayName }) {
	return patientDocumentService.initializeDocumentUpload({ token, fileName, contentType, documentType, documentSubType, displayName })
}

export async function uploadDocumentToPresignedUrl({ uploadUrl, file, contentType }) {
	return patientDocumentService.uploadDocumentToPresignedUrl({ uploadUrl, file, contentType })
}

export async function getMyDocumentDownloadUrl({ token, documentId }) {
	return patientDocumentService.getMyDocumentDownloadUrl({ token, documentId })
}

export async function deleteMyDocument({ token, documentId }) {
	return patientDocumentService.deleteMyDocument({ token, documentId })
}

export async function getMyDocumentAccess({ token }) {
	if (!token) throw new Error('token is required')

	return request('/patients/me/documents/access', {
		headers: authHeaders(token),
	})
}

export async function getDoctorProfile(doctorId) {
	return getDoctorById(doctorId)
}

export default {
	getMyDocuments,
	initializeDocumentUpload,
	uploadDocumentToPresignedUrl,
	getMyDocumentDownloadUrl,
	deleteMyDocument,
	getMyDocumentAccess,
	getDoctorProfile,
}
