const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
).replace(/\/$/, '')

function buildHeaders({ token, userId, role, userEmail } = {}) {
  return {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(userId ? { 'X-User-ID': userId } : {}),
    ...(role ? { 'X-User-Role': role } : {}),
    ...(userEmail ? { 'X-User-Email': userEmail } : {}),
  }
}

function extractErrorMessage(payload) {
  if (typeof payload === 'string') return payload
  if (!payload || typeof payload !== 'object') return 'Request failed'
  if (payload.message) return payload.message
  if (payload.error) return payload.error
  if (payload.errors && typeof payload.errors === 'object') {
    const first = Object.values(payload.errors)[0]
    if (first) return String(first)
  }
  return 'Request failed'
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
    const error = new Error(extractErrorMessage(payload))
    error.status = response.status
    error.payload = payload
    throw error
  }

  return payload
}

export async function listAppointments({ token, userId, role = 'PATIENT', userEmail } = {}) {
  return request('/appointments', {
    headers: buildHeaders({ token, userId, role, userEmail }),
  })
}

export async function getAppointmentById({ token, userId, appointmentId, role = 'PATIENT', userEmail } = {}) {
  return request(`/appointments/${encodeURIComponent(appointmentId)}`, {
    headers: buildHeaders({ token, userId, role, userEmail }),
  })
}

export async function getAvailableSlots({ scheduleId, date } = {}) {
  return request(`/appointments/slots?scheduleId=${encodeURIComponent(scheduleId)}&date=${encodeURIComponent(date)}`)
}

export async function getDoctorAvailableSlots({ doctorId, type, date } = {}) {
  const query = new URLSearchParams()
  if (typeof type === 'string' && type.trim()) {
    query.set('type', type.trim())
  }
  if (typeof date === 'string' && date.trim()) {
    query.set('date', date.trim())
  }

  const suffix = query.toString() ? `?${query.toString()}` : ''
  return request(`/appointments/slots/doctor/${encodeURIComponent(doctorId)}${suffix}`)
}

export async function bookAppointment({ token, userId, userEmail, role = 'PATIENT', payload } = {}) {
  return request('/appointments', {
    method: 'POST',
    headers: buildHeaders({ token, userId, role, userEmail }),
    body: JSON.stringify(payload || {}),
  })
}

export async function cancelAppointment({ token, userId, appointmentId, role = 'PATIENT', userEmail } = {}) {
  return request(`/appointments/${encodeURIComponent(appointmentId)}/cancel`, {
    method: 'PATCH',
    headers: buildHeaders({ token, userId, role, userEmail }),
  })
}

export async function getPendingVideoAppointments({ token, userId, role = 'DOCTOR', userEmail } = {}) {
  return request('/appointments/pending-video', {
    headers: buildHeaders({ token, userId, role, userEmail }),
  })
}

export async function updateAppointmentStatus({ token, userId, appointmentId, status, role = 'DOCTOR', userEmail } = {}) {
  return request(`/appointments/${encodeURIComponent(appointmentId)}/status`, {
    method: 'PATCH',
    headers: buildHeaders({ token, userId, role, userEmail }),
    body: JSON.stringify({ status }),
  })
}

export async function completeAppointment({ token, userId, appointmentId, role = 'DOCTOR', userEmail } = {}) {
  return request(`/appointments/${encodeURIComponent(appointmentId)}/complete`, {
    method: 'PATCH',
    headers: buildHeaders({ token, userId, role, userEmail }),
  })
}

export default {
  listAppointments,
  getAppointmentById,
  getAvailableSlots,
  getDoctorAvailableSlots,
  bookAppointment,
  cancelAppointment,
  getPendingVideoAppointments,
  updateAppointmentStatus,
  completeAppointment,
}
