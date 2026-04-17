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

export async function initiatePayment({ token, userId, userEmail, role = 'PATIENT', payload } = {}) {
  return request('/payments/initiate', {
    method: 'POST',
    headers: buildHeaders({ token, userId, role, userEmail }),
    body: JSON.stringify(payload || {}),
  })
}

export async function getPaymentById({ token, userId, paymentId, role = 'PATIENT', userEmail } = {}) {
  return request(`/payments/${encodeURIComponent(paymentId)}`, {
    headers: buildHeaders({ token, userId, role, userEmail }),
  })
}

export async function getPaymentByAppointment({ token, userId, appointmentId, role = 'PATIENT', userEmail } = {}) {
  return request(`/payments/appointment/${encodeURIComponent(appointmentId)}`, {
    headers: buildHeaders({ token, userId, role, userEmail }),
  })
}

export async function listPayments({ token, userId, role = 'PATIENT', userEmail } = {}) {
  return request('/payments', {
    headers: buildHeaders({ token, userId, role, userEmail }),
  })
}

export default {
  initiatePayment,
  getPaymentById,
  getPaymentByAppointment,
  listPayments,
}
