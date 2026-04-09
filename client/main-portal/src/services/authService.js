const API_BASE_URL = (
	import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
).replace(/\/$/, '')

async function request(path, options = {}) {
	const response = await fetch(`${API_BASE_URL}${path}`, {
		credentials: 'include',
		headers: {
			'Content-Type': 'application/json',
			...(options.headers || {}),
		},
		...options,
	})

	const contentType = response.headers.get('content-type') || ''
	const payload = contentType.includes('application/json')
		? await response.json()
		: await response.text()

	if (!response.ok) {
		const message = typeof payload === 'string'
			? payload
			: payload?.message || payload?.error || 'Request failed'
		throw new Error(message)
	}

	return payload
}

export async function login({ email, password }) {
	return request('/auth/login', {
		method: 'POST',
		body: JSON.stringify({ email, password }),
	})
}

export async function register({ email, password, role = 'PATIENT' }) {
	return request('/auth/register', {
		method: 'POST',
		body: JSON.stringify({ email, password, role }),
	})
}

export async function refresh() {
	return request('/auth/refresh', {
		method: 'POST',
	})
}

export async function logout() {
	return request('/auth/logout', {
		method: 'POST',
	})
}

export default {
	login,
	register,
	refresh,
	logout,
}
