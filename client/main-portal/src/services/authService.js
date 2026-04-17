const API_BASE_URL = (
	import.meta.env.VITE_API_BASE_URL ||
	(window.location.hostname === 'localhost' ? 'http://localhost:8080' : '')
).replace(/\/$/, '')


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

export async function deactivateSelf({ reason, token }) {
	const query = reason ? `?reason=${encodeURIComponent(reason)}` : ''
	return request(`/auth/deactivate${query}`, {
		method: 'POST',
		headers: {
			Authorization: `Bearer ${token}`,
		},
	})
}

export default {
	login,
	register,
	refresh,
	logout,
	deactivateSelf,
}
