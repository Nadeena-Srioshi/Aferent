const API_BASE_URL = (
	import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
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

export async function getSpecializations() {
	return request('/specializations')
}

export async function getHospitals() {
	return request('/hospitals')
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

export async function getLicenseUploadUrl({ authId, fileName, contentType }) {
	const params = new URLSearchParams({ authId, fileName, contentType })
	return request(`/doctors/register/license-upload-url?${params}`, {
		method: 'POST',
	})
}

export async function confirmLicenseUpload({ authId, objectKey }) {
	const params = new URLSearchParams({ authId, objectKey })
	return request(`/doctors/register/license-confirm?${params}`, {
		method: 'POST',
	})
}

export async function getDoctorById(doctorId) {
	if (!doctorId) throw new Error('doctorId is required')

	return request(`/doctors/${encodeURIComponent(doctorId)}`)
}

export default {
	getSpecializations,
	getHospitals,
	registerDoctorAuth,
	completeDoctorProfile,
	getLicenseUploadUrl,
	confirmLicenseUpload,
	getDoctorById,
}