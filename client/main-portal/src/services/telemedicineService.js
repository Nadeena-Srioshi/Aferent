const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ||
  (window.location.hostname === 'localhost' ? 'http://localhost:8080' : '')
).replace(/\/$/, '')


function buildHeaders({ token, userId, role, userEmail } = {}) {
  return {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(userId ? { 'X-User-ID': userId } : {}),
    ...(role ? { 'X-User-Role': role } : {}),
    ...(userEmail ? { 'X-User-Email': userEmail } : {}),
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
    const msg =
      typeof payload === 'string'
        ? payload
        : payload?.message || payload?.error || 'Request failed'
    const error = new Error(msg)
    error.status = response.status
    error.payload = payload
    throw error
  }

  return payload
}

export async function joinSession({ token, userId, role, userEmail, appointmentId } = {}) {
  return request(`/sessions/join/${encodeURIComponent(appointmentId)}`, {
    method: 'POST',
    headers: buildHeaders({ token, userId, role, userEmail }),
  })
}

export async function getSessionStatus({ token, userId, role, userEmail, appointmentId } = {}) {
  return request(`/sessions/${encodeURIComponent(appointmentId)}/status`, {
    headers: buildHeaders({ token, userId, role, userEmail }),
  })
}

export async function getRemainingDuration({ token, userId, role, userEmail, appointmentId } = {}) {
  return request(`/sessions/${encodeURIComponent(appointmentId)}/remaining-duration`, {
    headers: buildHeaders({ token, userId, role, userEmail }),
  })
}

export async function endSession({ token, userId, role, userEmail, appointmentId } = {}) {
  return request(`/sessions/${encodeURIComponent(appointmentId)}/end`, {
    method: 'POST',
    headers: buildHeaders({ token, userId, role, userEmail }),
  })
}

export default {
  joinSession,
  getSessionStatus,
  getRemainingDuration,
  endSession,
}
