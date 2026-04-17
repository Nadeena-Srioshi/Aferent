const API_BASE_URL = (
	import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
).replace(/\/$/, '')

function authHeaders(token) {
	return {
		Authorization: `Bearer ${token}`,
	}
}

function identityHeaders({ authId, role, patientId }) {
	const headers = {}
	if (authId) headers['X-User-ID'] = authId
	if (role) headers['X-User-Role'] = role
	if (patientId) headers['X-Patient-ID'] = patientId
	return headers
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

export async function getMyMedicalHistory({ token }) {
	if (!token) throw new Error('token is required')

	return request('/patients/me/medical-history', {
		headers: authHeaders(token),
	})
}

// Endpoint provided by doctor-service via api-gateway.
// We fetch this only on user action (click), not during page load.
export async function getPrescriptionQrSignedUrl({ token, prescriptionId, authId, role, patientId }) {
	if (!token) throw new Error('token is required')
	if (!prescriptionId) throw new Error('prescriptionId is required')

	const payload = await request(`/prescriptions/${encodeURIComponent(prescriptionId)}/qr/signed-url`, {
		headers: {
			...authHeaders(token),
			...identityHeaders({ authId, role, patientId }),
		},
	})

	const signedUrl =
		payload?.signedUrl ||
		payload?.qrSignedUrl ||
		payload?.downloadUrl ||
		payload?.url ||
		null

	if (!signedUrl) {
		throw new Error('QR signed URL was not returned by server')
	}

	return { signedUrl, raw: payload }
}

export default {
	getMyMedicalHistory,
	getPrescriptionQrSignedUrl,
}
