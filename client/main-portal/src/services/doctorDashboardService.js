const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
).replace(/\/$/, '')

function authHeaders({ token, role = 'DOCTOR', authId = '' }) {
  return {
    Authorization: `Bearer ${token}`,
    'X-User-Role': role,
    ...(authId ? { 'X-User-ID': authId } : {}),
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

function toArray(value) {
  return Array.isArray(value) ? value : []
}

function sortByDateDesc(items, dateKey) {
  return [...items].sort((a, b) => {
    const left = new Date(a?.[dateKey] || 0).getTime()
    const right = new Date(b?.[dateKey] || 0).getTime()
    return right - left
  })
}

function sameCalendarDay(left, right = new Date()) {
  const date = new Date(left)
  if (Number.isNaN(date.getTime())) return false
  return date.toDateString() === right.toDateString()
}

function minutesBetween(start, end) {
  if (!start || !end) return 0
  const [sh, sm] = String(start).split(':').map(Number)
  const [eh, em] = String(end).split(':').map(Number)
  if ([sh, sm, eh, em].some((n) => Number.isNaN(n))) return 0
  return Math.max(0, (eh * 60 + em) - (sh * 60 + sm))
}

function calculateWeeklyHours(schedule) {
  if (!schedule || typeof schedule !== 'object') return 0
  const dayKeys = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday']
  const totalMinutes = dayKeys.reduce((sum, dayKey) => {
    const slots = toArray(schedule[dayKey])
    const dayMinutes = slots.reduce((slotSum, slot) => slotSum + minutesBetween(slot?.startTime, slot?.endTime), 0)
    return sum + dayMinutes
  }, 0)

  return Number((totalMinutes / 60).toFixed(1))
}

export async function getDoctors({ token }) {
  return request('/doctors', {
    headers: authHeaders({ token }),
  })
}

export async function resolveDoctorIdentity({ token, authId, email }) {
  const doctors = toArray(await getDoctors({ token }))
  if (!doctors.length) return null

  const byAuthId = doctors.find((doctor) => doctor?.authId && doctor.authId === authId)
  if (byAuthId) return byAuthId

  const normalizedEmail = String(email || '').toLowerCase()
  const byEmail = doctors.find((doctor) =>
    String(doctor?.email || '').toLowerCase() === normalizedEmail,
  )

  return byEmail || doctors[0]
}

export async function getDoctorAppointments({ token, authId }) {
  return request('/appointments', {
    headers: authHeaders({ token, authId }),
  })
}

export async function getPendingVideoAppointments({ token, authId }) {
  return request('/appointments/pending-video', {
    headers: authHeaders({ token, authId }),
  })
}

export async function getDoctorPatients({ token, doctorId, authId }) {
  return request(`/doctors/${doctorId}/patients`, {
    headers: authHeaders({ token, authId }),
  })
}

export async function getWeeklySchedule({ token, doctorId, authId }) {
  return request(`/doctors/${doctorId}/schedule/weekly`, {
    headers: authHeaders({ token, authId }),
  })
}

export async function getDoctorDashboardData({ token, authId, email }) {
  const doctor = await resolveDoctorIdentity({ token, authId, email })

  const [appointmentsResult, pendingResult, patientsResult, scheduleResult] = await Promise.allSettled([
    getDoctorAppointments({ token, authId }),
    getPendingVideoAppointments({ token, authId }),
    doctor?.doctorId ? getDoctorPatients({ token, doctorId: doctor.doctorId, authId }) : Promise.resolve([]),
    doctor?.doctorId ? getWeeklySchedule({ token, doctorId: doctor.doctorId, authId }) : Promise.resolve(null),
  ])

  const appointments = toArray(appointmentsResult.status === 'fulfilled' ? appointmentsResult.value : [])
  const pendingVideo = toArray(pendingResult.status === 'fulfilled' ? pendingResult.value : [])
  const recentPatients = sortByDateDesc(
    toArray(patientsResult.status === 'fulfilled' ? patientsResult.value : []),
    'lastVisitDate',
  )
  const weeklySchedule = scheduleResult.status === 'fulfilled' ? scheduleResult.value : null

  const upcomingAppointments = sortByDateDesc(appointments, 'appointmentDate')
    .filter((item) => {
      const time = new Date(item?.appointmentDate || 0).getTime()
      return !Number.isNaN(time) && time >= Date.now() - 30 * 60 * 1000
    })
    .reverse()

  const uniquePatientIds = new Set(
    appointments.map((item) => item?.patientId).filter(Boolean),
  )

  const stats = {
    appointmentsToday: appointments.filter((item) => sameCalendarDay(item?.appointmentDate)).length,
    pendingApprovals: pendingVideo.length,
    uniquePatients: uniquePatientIds.size,
    weeklyHours: calculateWeeklyHours(weeklySchedule),
  }

  return {
    doctor,
    stats,
    upcomingAppointments,
    pendingVideo,
    recentPatients,
    weeklySchedule,
    partialFailures: {
      appointments: appointmentsResult.status === 'rejected',
      pendingVideo: pendingResult.status === 'rejected',
      patients: patientsResult.status === 'rejected',
      schedule: scheduleResult.status === 'rejected',
    },
  }
}

export default {
  getDoctorDashboardData,
}
