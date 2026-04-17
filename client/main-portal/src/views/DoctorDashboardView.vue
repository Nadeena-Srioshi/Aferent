<template>
	<main id="main-content" class="min-h-screen bg-surface">
		<div class="max-w-7xl mx-auto px-4 sm:px-6 py-8 sm:py-10">
			<div class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between mb-8">
				<div>
					<p class="text-sm font-medium text-primary uppercase tracking-wide mb-1">Doctor Workspace</p>
					<h1 class="text-3xl font-bold text-ink">Welcome back, {{ doctorName }}</h1>
					<p class="text-sm text-muted mt-2">
						{{ doctorSpecialization }}
						<span v-if="doctorStatus" class="ml-2 px-2 py-0.5 rounded-full text-xs font-semibold bg-primary/10 text-primary">
							{{ doctorStatus }}
						</span>
					</p>
				</div>
				<div class="flex flex-wrap gap-2">
					<RouterLink
						to="/doctor/schedule"
						class="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl border border-border bg-card text-sm font-semibold text-ink hover:border-primary/40 hover:text-primary transition-colors"
					>
						<Clock3 class="w-4 h-4" aria-hidden="true" />
						Manage Availability
					</RouterLink>
					<RouterLink
						to="/doctor/profile"
						class="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl border border-border bg-card text-sm font-semibold text-ink hover:border-primary/40 hover:text-primary transition-colors"
					>
						<UserRound class="w-4 h-4" aria-hidden="true" />
						Edit Profile
					</RouterLink>
					<RouterLink
						to="/appointments"
						class="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-primary text-white text-sm font-semibold hover:bg-action transition-colors"
					>
						<CalendarDays class="w-4 h-4" aria-hidden="true" />
						View Appointments
					</RouterLink>
				</div>
			</div>

			<div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4 mb-8">
				<div
					v-for="n in 4"
					:key="n"
					class="rounded-2xl border border-border bg-card p-5 animate-pulse"
				>
					<div class="h-3 w-28 bg-surface rounded mb-4"></div>
					<div class="h-8 w-20 bg-surface rounded"></div>
				</div>
			</div>

			<div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4 mb-8">
				<article class="rounded-2xl border border-border bg-card p-5">
					<div class="flex items-center justify-between mb-3">
						<p class="text-sm text-muted">Today&apos;s appointments</p>
						<CalendarDays class="w-4 h-4 text-primary" aria-hidden="true" />
					</div>
					<p class="text-3xl font-bold text-ink">{{ stats.appointmentsToday }}</p>
				</article>
				<article class="rounded-2xl border border-border bg-card p-5">
					<div class="flex items-center justify-between mb-3">
						<p class="text-sm text-muted">Pending video approvals</p>
						<Video class="w-4 h-4 text-primary" aria-hidden="true" />
					</div>
					<p class="text-3xl font-bold text-ink">{{ stats.pendingApprovals }}</p>
				</article>
				<article class="rounded-2xl border border-border bg-card p-5">
					<div class="flex items-center justify-between mb-3">
						<p class="text-sm text-muted">Unique patients</p>
						<Users class="w-4 h-4 text-primary" aria-hidden="true" />
					</div>
					<p class="text-3xl font-bold text-ink">{{ stats.uniquePatients }}</p>
				</article>
				<article class="rounded-2xl border border-border bg-card p-5">
					<div class="flex items-center justify-between mb-3">
						<p class="text-sm text-muted">Hours this week</p>
						<Clock3 class="w-4 h-4 text-primary" aria-hidden="true" />
					</div>
					<p class="text-3xl font-bold text-ink">{{ stats.weeklyHours }}</p>
				</article>
			</div>

			<div v-if="error" class="rounded-2xl border border-danger/20 bg-red-50 p-4 mb-8 flex items-center justify-between gap-3">
				<p class="text-sm text-danger">{{ error }}</p>
				<button
					type="button"
					class="px-4 py-2 rounded-xl border border-danger/30 text-danger text-sm font-semibold hover:bg-danger/5"
					@click="loadDashboard"
				>
					Retry
				</button>
			</div>

			<section class="grid grid-cols-1 lg:grid-cols-3 gap-5">
				<article class="lg:col-span-2 rounded-2xl border border-border bg-card p-5">
					<div class="flex items-center justify-between mb-4">
						<h2 class="text-lg font-semibold text-ink">Upcoming appointments</h2>
						<RouterLink to="/appointments" class="text-sm font-medium text-primary hover:underline">
							See all
						</RouterLink>
					</div>

					<div v-if="upcomingAppointments.length === 0" class="rounded-xl border border-dashed border-border p-6 text-center">
						<p class="text-sm text-muted">No upcoming appointments right now.</p>
					</div>

					<ul v-else class="divide-y divide-border">
						<li
							v-for="appointment in upcomingAppointments"
							:key="appointment.id"
							class="py-3 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between"
						>
							<div>
								<p class="text-sm font-semibold text-ink">{{ appointment.patientName || 'Patient' }}</p>
								<p class="text-xs text-muted">{{ formatDateTime(appointment.appointmentDate) }}</p>
							</div>
							<span
								class="self-start sm:self-auto px-2.5 py-1 rounded-full text-xs font-semibold"
								:class="statusClass(appointment.status)"
							>
								{{ appointment.status || 'SCHEDULED' }}
							</span>
						</li>
					</ul>
				</article>

				<article class="rounded-2xl border border-border bg-card p-5">
					<h2 class="text-lg font-semibold text-ink mb-4">Recent patients</h2>

					<div v-if="recentPatients.length === 0" class="rounded-xl border border-dashed border-border p-6 text-center">
						<p class="text-sm text-muted">No recent patient activity.</p>
					</div>

					<ul v-else class="space-y-3">
						<li
							v-for="patient in recentPatients"
							:key="patient.patientId"
							class="flex items-center gap-3"
						>
							<div class="w-9 h-9 rounded-full bg-primary/10 text-primary text-xs font-bold flex items-center justify-center shrink-0">
								{{ initials(patient.patientName) }}
							</div>
							<div class="min-w-0">
								<p class="text-sm font-semibold text-ink truncate">{{ patient.patientName || 'Patient' }}</p>
								<p class="text-xs text-muted">
									{{ patient.visitCount || 0 }} visits
									<span v-if="patient.lastVisitDate">· {{ formatDateTime(patient.lastVisitDate) }}</span>
								</p>
							</div>
						</li>
					</ul>
				</article>
			</section>

			<section class="mt-5 grid grid-cols-1 lg:grid-cols-3 gap-5">
				<article class="lg:col-span-2 rounded-2xl border border-border bg-card p-5">
					<h2 class="text-lg font-semibold text-ink mb-4">Weekly schedule snapshot</h2>
					<div class="grid grid-cols-2 sm:grid-cols-4 xl:grid-cols-7 gap-2">
						<div
							v-for="day in weekSummary"
							:key="day.label"
							class="rounded-xl border border-border bg-surface px-3 py-4 text-center"
						>
							<p class="text-xs font-semibold text-muted uppercase">{{ day.label }}</p>
							<p class="text-lg font-bold text-ink mt-1">{{ day.slots }}</p>
							<p class="text-xs text-muted">{{ day.slots === 1 ? 'session' : 'sessions' }}</p>
						</div>
					</div>
				</article>

				<article class="rounded-2xl border border-border bg-card p-5">
					<h2 class="text-lg font-semibold text-ink mb-4">Pending requests</h2>
					<div v-if="pendingVideo.length === 0" class="rounded-xl border border-dashed border-border p-6 text-center">
						<p class="text-sm text-muted">No pending video requests.</p>
					</div>
					<ul v-else class="space-y-3">
						<li
							v-for="appointment in pendingVideo"
							:key="appointment.id"
							class="rounded-xl border border-border p-3"
						>
							<p class="text-sm font-semibold text-ink">{{ appointment.patientName || 'Patient' }}</p>
							<p class="text-xs text-muted mt-0.5">{{ formatDateTime(appointment.appointmentDate) }}</p>
						</li>
					</ul>
				</article>
			</section>
		</div>
	</main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Clock3, CalendarDays, Users, Video, UserRound } from 'lucide-vue-next'
import { useAuth } from '@/stores/useAuth'
import doctorDashboardService from '@/services/doctorDashboardService'

const auth = useAuth()

const loading = ref(true)
const error = ref('')
const doctor = ref(null)
const stats = ref({
	appointmentsToday: 0,
	pendingApprovals: 0,
	uniquePatients: 0,
	weeklyHours: 0,
})
const upcomingAppointments = ref([])
const recentPatients = ref([])
const pendingVideo = ref([])
const weeklySchedule = ref(null)

const doctorName = computed(() => {
	const firstName = doctor.value?.firstName || ''
	const lastName = doctor.value?.lastName || ''
	const fullName = `${firstName} ${lastName}`.trim()
	return fullName || auth.fullName || 'Doctor'
})

const doctorSpecialization = computed(() => doctor.value?.specialization || 'General Practice')
const doctorStatus = computed(() => doctor.value?.status || '')

const weekSummary = computed(() => {
	const labels = [
		{ key: 'monday', label: 'Mon' },
		{ key: 'tuesday', label: 'Tue' },
		{ key: 'wednesday', label: 'Wed' },
		{ key: 'thursday', label: 'Thu' },
		{ key: 'friday', label: 'Fri' },
		{ key: 'saturday', label: 'Sat' },
		{ key: 'sunday', label: 'Sun' },
	]

	return labels.map((day) => ({
		label: day.label,
		slots: Array.isArray(weeklySchedule.value?.[day.key]) ? weeklySchedule.value[day.key].length : 0,
	}))
})

function initials(name) {
	return String(name || '')
		.split(' ')
		.filter(Boolean)
		.slice(0, 2)
		.map((part) => part[0].toUpperCase())
		.join('') || 'P'
}

function statusClass(status) {
	const value = String(status || '').toUpperCase()
	if (value.includes('CONFIRMED') || value.includes('ACCEPTED')) {
		return 'bg-green-50 text-green-700'
	}
	if (value.includes('PENDING')) {
		return 'bg-amber-50 text-amber-700'
	}
	if (value.includes('CANCEL') || value.includes('REJECT')) {
		return 'bg-red-50 text-red-700'
	}
	return 'bg-primary/10 text-primary'
}

function formatDateTime(value) {
	const date = new Date(value)
	if (Number.isNaN(date.getTime())) return 'Date unavailable'
	return new Intl.DateTimeFormat('en-US', {
		month: 'short',
		day: 'numeric',
		hour: 'numeric',
		minute: '2-digit',
	}).format(date)
}

async function loadDashboard() {
	if (!auth.token) {
		error.value = 'No active session found. Please sign in again.'
		return
	}

	loading.value = true
	error.value = ''

	try {
		const data = await doctorDashboardService.getDoctorDashboardData({
			token: auth.token,
			authId: auth.user?.authId,
			email: auth.user?.email,
		})

		doctor.value = data.doctor
		stats.value = data.stats
		upcomingAppointments.value = data.upcomingAppointments.slice(0, 6)
		recentPatients.value = data.recentPatients.slice(0, 6)
		pendingVideo.value = data.pendingVideo.slice(0, 4)
		weeklySchedule.value = data.weeklySchedule
	} catch (e) {
		error.value = e?.message || 'Unable to load dashboard right now.'
	} finally {
		loading.value = false
	}
}

onMounted(() => {
	loadDashboard()
})
</script>
