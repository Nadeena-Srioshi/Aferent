const API_BASE_URL = (
	import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
).replace(/\/$/, '')

function authHeaders(token) {
	return token ? { Authorization: `Bearer ${token}` } : {}
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

export async function getSpecializations() {
	return request('/specializations')
}

export async function getHospitals() {
	return request('/hospitals')
}

export async function getDoctorProfile({ token, doctorId }) {
	return request(`/doctors/${doctorId}`, {
		headers: authHeaders(token),
	})
}

export async function updateDoctorProfile({ token, doctorId, payload }) {
	return request(`/doctors/${doctorId}/profile`, {
		method: 'PUT',
		headers: authHeaders(token),
		body: JSON.stringify(payload),
	})
}

export async function registerDoctorAuth({ email, password }) {
	return request('/auth/register', {
		method: 'POST',
		body: JSON.stringify({ email, password, role: 'DOCTOR' }),
	})
}

export async function completeDoctorProfile(payload) {
	return request('/doctors/register/profile', {
		method: 'POST',
		body: JSON.stringify(payload),
	})
}

export async function getLicenseUploadUrl({ authId, fileName }) {
	return request('/doctors/license-upload-url', {
		method: 'POST',
		body: JSON.stringify({ authId, fileName }),
	})
}

export async function getProfilePicUploadUrl({ token, fileName }) {
	const params = new URLSearchParams({ fileName })
	return request(`/doctors/profile/pic-upload-url?${params}`, {
		method: 'POST',
		headers: authHeaders(token),
	})
}

export async function getProfilePicUrl({ doctorId }) {
	return request(`/doctors/${doctorId}/profile/pic-url`)
}

export async function getLicenseSignedUrl({ token, doctorId, expires = 3600 }) {
	const params = new URLSearchParams({ expires: String(expires) })
	return request(`/doctors/${doctorId}/license/signed-url?${params}`, {
		headers: authHeaders(token),
	})
}

export async function getWeeklySchedule({ doctorId }) {
	return request(`/doctors/${doctorId}/schedule/weekly`)
}

export async function setWeeklySchedule({ token, doctorId, payload }) {
	return request(`/doctors/${doctorId}/schedule/weekly`, {
		method: 'PUT',
		headers: authHeaders(token),
		body: JSON.stringify(payload),
	})
}

export async function deleteWeeklySchedule({ token, doctorId }) {
	return request(`/doctors/${doctorId}/schedule/weekly`, {
		method: 'DELETE',
		headers: authHeaders(token),
	})
}

export async function getScheduleOverrides({ doctorId }) {
	return request(`/doctors/${doctorId}/schedule/overrides`)
}

export async function addScheduleOverride({ token, doctorId, payload }) {
	return request(`/doctors/${doctorId}/schedule/overrides`, {
		method: 'POST',
		headers: authHeaders(token),
		body: JSON.stringify(payload),
	})
}

export async function deleteScheduleOverride({ token, doctorId, overrideId }) {
	return request(`/doctors/${doctorId}/schedule/overrides/${overrideId}`, {
		method: 'DELETE',
		headers: authHeaders(token),
	})
}

export async function uploadFileToPresignedUrl({ uploadUrl, file }) {
	const headers = file?.type ? { 'Content-Type': file.type } : undefined
	const response = await fetch(uploadUrl, {
		method: 'PUT',
		headers,
		body: file,
	})

	if (!response.ok) {
		const payload = await response.text()
		const message = payload || 'License upload failed'
		const error = new Error(message)
		error.status = response.status
		throw error
	}

	return true
}

export default {
	getSpecializations,
	getHospitals,
	getDoctorProfile,
	updateDoctorProfile,
	registerDoctorAuth,
	completeDoctorProfile,
	getLicenseUploadUrl,
	getProfilePicUploadUrl,
	getProfilePicUrl,
	getLicenseSignedUrl,
	getWeeklySchedule,
	setWeeklySchedule,
	deleteWeeklySchedule,
	getScheduleOverrides,
	addScheduleOverride,
	deleteScheduleOverride,
	uploadFileToPresignedUrl,
}