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
		const message = typeof payload === 'string'
			? payload
			: payload?.message || payload?.error || 'Request failed'
		const error = new Error(message)
		error.status = response.status
		throw error
	}

	return payload
}

export async function getCurrentProfile({ token }) {
	return request('/patients/me', {
		headers: authHeaders(token),
	})
}

export async function getProfile({ patientId, token }) {
	return request(`/patients/${patientId}`, {
		headers: authHeaders(token),
	})
}

export async function updateProfile({ patientId, token, payload }) {
	return request(`/patients/${patientId}`, {
		method: 'PUT',
		headers: authHeaders(token),
		body: JSON.stringify(payload),
	})
}

export async function resolvePatientId({ token }) {
	const me = await getCurrentProfile({ token })
	return me?.patientId || null
}

export default {
	getCurrentProfile,
	getProfile,
	updateProfile,
	resolvePatientId,
}
