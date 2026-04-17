<template>
	<main class="min-h-screen bg-[radial-gradient(circle_at_top,rgba(0,77,81,0.12),transparent_32%),linear-gradient(180deg,#f7fbfb_0%,#f2f7f6_100%)]">
		<div class="max-w-7xl mx-auto px-4 sm:px-6 py-6 sm:py-10">
			<div v-if="loading" class="flex items-center justify-center min-h-[70vh]">
				<div class="flex flex-col items-center gap-3">
					<div class="w-10 h-10 border-3 border-primary/20 border-t-primary rounded-full animate-spin"></div>
					<p class="text-sm text-muted">Loading doctor profile…</p>
				</div>
			</div>

			<div v-else-if="fetchError" class="flex items-center justify-center min-h-[70vh] p-6">
				<div class="text-center max-w-md bg-card border border-border rounded-3xl p-8 shadow-sm">
					<div class="w-14 h-14 bg-red-50 rounded-2xl flex items-center justify-center mx-auto mb-4">
						<AlertCircle class="w-7 h-7 text-danger" />
					</div>
					<h2 class="text-base font-semibold text-ink mb-1">Couldn't load your profile</h2>
					<p class="text-sm text-muted mb-4">{{ fetchError }}</p>
					<button
						type="button"
						@click="loadProfile"
						class="px-5 py-2.5 bg-primary text-white text-sm font-semibold rounded-xl hover:bg-action transition-colors"
					>
						Try again
					</button>
				</div>
			</div>

			<div v-else-if="doctor" class="space-y-6 sm:space-y-8">
				<section class="relative overflow-hidden rounded-[30px] border border-border bg-card shadow-[0_24px_60px_rgba(0,0,0,0.08)]">
					<div class="absolute inset-0 pointer-events-none">
						<div class="absolute -top-10 right-0 w-72 h-72 rounded-full bg-primary/10 blur-3xl"></div>
						<div class="absolute bottom-0 left-1/3 w-72 h-72 rounded-full bg-action/10 blur-3xl"></div>
					</div>

					<div class="relative z-10 p-5 sm:p-8">
						<div class="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
							<div class="flex flex-col sm:flex-row sm:items-end gap-4 sm:gap-5">
								<div class="relative w-fit">
									<div class="w-24 h-24 sm:w-28 sm:h-28 rounded-3xl border-4 border-white bg-primary/10 flex items-center justify-center overflow-hidden shadow-md">
										<img
											v-if="doctor.profilePicUrl"
											:src="doctor.profilePicUrl"
											:alt="fullName"
											class="w-full h-full object-cover"
										/>
										<span v-else class="text-3xl font-bold text-primary select-none">{{ initials }}</span>
									</div>
									<button
										type="button"
										@click="triggerProfilePicUpload"
										class="absolute -bottom-1 -right-1 w-8 h-8 rounded-xl bg-primary text-white flex items-center justify-center shadow-lg hover:bg-action transition-colors"
										title="Change profile photo"
									>
										<Camera class="w-4 h-4" />
									</button>
									<input ref="profilePicInput" type="file" accept="image/*" class="hidden" @change="onProfilePicChange" />
								</div>

								<div class="space-y-2">
									<div class="flex flex-wrap items-center gap-2">
										<span class="px-2.5 py-1 text-xs font-semibold rounded-full bg-primary/10 text-primary">{{ doctor.doctorId }}</span>
										<span class="px-2.5 py-1 text-xs font-semibold rounded-full" :class="statusClass">{{ doctor.status || 'UNKNOWN' }}</span>
									</div>
									<h1 class="text-3xl sm:text-4xl font-semibold text-ink tracking-tight">{{ fullName }}</h1>
									<p class="text-sm text-muted">{{ doctor.email }}</p>
									<p class="text-sm text-muted">
										<span v-if="specializationName">{{ specializationName }}</span>
										<span v-else>Specialization not set</span>
										<span v-if="doctor.licenseNumber" class="ml-2">• {{ doctor.licenseNumber }}</span>
									</p>
								</div>
							</div>

							<div class="flex flex-wrap gap-2 lg:justify-end">
								<RouterLink
									to="/doctor/dashboard"
									class="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl border border-border bg-white/80 backdrop-blur text-sm font-semibold text-ink hover:border-primary/40 hover:text-primary transition-colors"
								>
									<ArrowLeft class="w-4 h-4" />
									Dashboard
								</RouterLink>
								<button
									type="button"
									@click="viewLicense"
									:disabled="licenseLoading"
									class="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-primary text-white text-sm font-semibold hover:bg-action transition-colors disabled:opacity-60"
								>
									<span v-if="licenseLoading" class="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
									<FileText v-else class="w-4 h-4" />
									View License
								</button>
							</div>
						</div>

						<div class="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-6">
							<div v-for="stat in quickStats" :key="stat.label" class="rounded-2xl border border-border/70 bg-white/70 backdrop-blur px-4 py-3">
								<div class="flex items-center justify-between gap-2">
									<p class="text-[11px] font-semibold uppercase tracking-wide text-muted">{{ stat.label }}</p>
									<component :is="stat.icon" class="w-4 h-4 text-primary" />
								</div>
								<p class="mt-2 text-sm font-semibold text-ink truncate">{{ stat.value || '—' }}</p>
							</div>
						</div>
					</div>
				</section>

				<section class="grid grid-cols-1 xl:grid-cols-12 gap-5">
					<article class="xl:col-span-8 bg-card rounded-[28px] border border-border p-5 sm:p-6 shadow-sm">
						<div class="flex items-center justify-between mb-5">
							<div>
								<p class="text-xs font-semibold uppercase tracking-[0.2em] text-primary mb-1">Editable details</p>
								<h2 class="text-xl font-semibold text-ink">Profile details</h2>
								<p class="text-sm text-muted mt-1">Update your professional profile information below.</p>
							</div>
							<button type="button" @click="resetForm" class="text-sm font-semibold text-primary hover:underline">Reset</button>
						</div>

						<div class="space-y-5">
							<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
								<div>
									<label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">First Name</label>
									<input v-model="form.firstName" type="text" class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors" />
								</div>
								<div>
									<label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Last Name</label>
									<input v-model="form.lastName" type="text" class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors" />
								</div>
							</div>

							<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
								<div>
									<label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Phone</label>
									<input v-model="form.phone" type="tel" class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors" />
								</div>
								<div>
									<label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Years of Experience</label>
									<input v-model.number="form.yearsOfExperience" type="number" min="0" max="60" class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors" />
								</div>
							</div>

							<div>
								<label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Bio</label>
								<textarea v-model="form.bio" rows="4" class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface resize-none focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors" placeholder="Tell patients about your practice"></textarea>
							</div>

							<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
								<div>
									<label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Qualifications</label>
									<textarea v-model="form.qualificationsText" rows="3" class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface resize-none focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors" placeholder="MBBS, MD, Fellowship"></textarea>
								</div>
								<div>
									<label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Languages</label>
									<input v-model="form.languagesText" type="text" class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors" placeholder="English, Sinhala, Tamil" />
								</div>
							</div>

							<div>
								<div class="flex items-center justify-between gap-3 mb-1.5">
									<label class="block text-xs font-semibold uppercase tracking-wide text-muted">Hospitals</label>
									<span class="text-xs font-medium text-muted">{{ form.hospitalIds.length }} selected</span>
								</div>
								<div class="mb-3">
									<input
										v-model="hospitalQuery"
										type="text"
										placeholder="Search hospitals by name or city"
										class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface placeholder-muted/50 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors"
									/>
								</div>
								<div class="max-h-72 overflow-y-auto rounded-2xl border border-border bg-surface p-2">
									<div v-if="filteredHospitals.length" class="grid grid-cols-1 sm:grid-cols-2 gap-2">
										<button
											v-for="hospital in filteredHospitals"
											:key="hospital.id"
											type="button"
											@click="toggleHospital(hospital.id)"
											class="text-left rounded-xl border p-3 transition-all"
											:class="form.hospitalIds.includes(hospital.id)
												? 'border-primary bg-primary/5 shadow-sm'
												: 'border-border bg-card hover:border-primary/30 hover:bg-primary/5'"
										>
											<div class="flex items-start gap-3">
												<div
													class="mt-0.5 w-5 h-5 rounded-full border flex items-center justify-center shrink-0"
													:class="form.hospitalIds.includes(hospital.id) ? 'border-primary bg-primary text-white' : 'border-border bg-white'"
												>
													<CheckCircle v-if="form.hospitalIds.includes(hospital.id)" class="w-3 h-3" />
												</div>
												<div class="min-w-0 flex-1">
													<p class="text-sm font-semibold text-ink truncate">{{ hospital.name }}</p>
													<p class="text-xs text-muted truncate">{{ hospital.city || 'City not specified' }}</p>
												</div>
											</div>
										</button>
									</div>
									<div v-else class="flex items-center justify-center py-8 text-center">
										<p class="text-sm text-muted">No active hospitals match your search.</p>
									</div>
								</div>
								<p class="text-xs text-muted mt-2">Doctors can belong to multiple hospitals. Search and tap the hospitals you work at.</p>
							</div>

							<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
								<div>
									<label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Specialization</label>
									<input :value="specializationName || doctor.specialization || '—'" type="text" readonly class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-muted bg-surface" />
								</div>
								<div>
									<label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">License Number</label>
									<input :value="doctor.licenseNumber || '—'" type="text" readonly class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-muted bg-surface" />
								</div>
							</div>

							<div v-if="formError" class="flex items-start gap-2.5 px-4 py-3 rounded-xl bg-red-50 border border-red-100">
								<AlertCircle class="w-4 h-4 text-danger shrink-0 mt-0.5" />
								<p class="text-xs text-danger leading-relaxed">{{ formError }}</p>
							</div>

							<div v-if="formSuccess" class="flex items-start gap-2.5 px-4 py-3 rounded-xl bg-green-50 border border-green-100">
								<CheckCircle class="w-4 h-4 text-success shrink-0 mt-0.5" />
								<p class="text-xs text-success leading-relaxed">Profile updated successfully.</p>
							</div>

							<div class="flex flex-col sm:flex-row gap-3 pt-1">
								<button
									type="button"
									@click="loadProfile"
									class="sm:w-40 py-3 text-sm font-semibold rounded-xl border border-border text-muted hover:bg-surface transition-colors"
								>
									Reload
								</button>
								<button
									type="button"
									@click="saveProfile"
									:disabled="saving"
									class="flex-1 py-3 text-white text-sm font-semibold rounded-xl transition-all disabled:opacity-60 disabled:cursor-not-allowed bg-primary hover:bg-action"
								>
									<span v-if="saving" class="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2 align-middle"></span>
									{{ saving ? 'Saving…' : 'Save changes' }}
								</button>
							</div>
						</div>
					</article>

					<aside class="xl:col-span-4 space-y-5">
						<article class="bg-card rounded-[28px] border border-border p-5 sm:p-6 shadow-sm">
							<h2 class="text-lg font-semibold text-ink mb-4">Overview</h2>
							<div class="space-y-4">
								<InfoRow label="Doctor ID" :value="doctor.doctorId" mono />
								<InfoRow label="Email" :value="doctor.email" />
								<InfoRow label="Status" :value="doctor.status" />
								<InfoRow label="Profile picture" :value="doctor.profilePicUrl ? 'Uploaded' : 'Not uploaded'" />
								<InfoRow label="License document" :value="doctor.licenseDocKey ? 'Available' : 'Not uploaded'" />
								<InfoRow label="Joined" :value="formattedCreatedAt" />
							</div>
						</article>

						<article class="bg-card rounded-[28px] border border-border p-5 sm:p-6 shadow-sm">
							<p class="text-xs font-semibold uppercase tracking-wide text-muted mb-3">Current selections</p>
							<div class="space-y-4">
								<div>
									<p class="text-xs text-muted mb-1">Hospitals</p>
									<div v-if="selectedHospitals.length" class="flex flex-wrap gap-2">
										<span v-for="hospital in selectedHospitals" :key="hospital.id" class="px-2.5 py-1 rounded-full bg-primary/10 text-primary text-xs font-semibold">
											{{ hospital.name }}
										</span>
									</div>
									<p v-else class="text-sm text-muted">No hospitals selected.</p>
								</div>

								<div>
									<p class="text-xs text-muted mb-1">Languages</p>
									<div v-if="languageList.length" class="flex flex-wrap gap-2">
										<span v-for="language in languageList" :key="language" class="px-2.5 py-1 rounded-full bg-surface border border-border text-xs font-semibold text-ink">
											{{ language }}
										</span>
									</div>
									<p v-else class="text-sm text-muted">No languages listed.</p>
								</div>

								<div>
									<p class="text-xs text-muted mb-1">Qualifications</p>
									<div v-if="qualificationList.length" class="space-y-2">
										<p v-for="qualification in qualificationList" :key="qualification" class="text-sm text-ink">
											• {{ qualification }}
										</p>
									</div>
									<p v-else class="text-sm text-muted">No qualifications listed.</p>
								</div>
							</div>

							<div v-if="licenseError" class="mt-5 flex items-start gap-2.5 px-4 py-3 rounded-xl bg-red-50 border border-red-100">
								<AlertCircle class="w-4 h-4 text-danger shrink-0 mt-0.5" />
								<p class="text-xs text-danger leading-relaxed">{{ licenseError }}</p>
							</div>
						</article>

						<article class="bg-card rounded-[28px] border border-border p-5 sm:p-6 shadow-sm">
							<h2 class="text-sm font-semibold text-ink flex items-center gap-2 mb-3">
								<Shield class="w-4 h-4 text-primary" />
								License Access
							</h2>
							<p class="text-sm text-muted">
								The backend stores your license privately. Use the button above to generate a signed view link when needed.
							</p>
							<div class="mt-4 flex flex-wrap gap-2">
								<span class="px-2.5 py-1 rounded-full bg-surface border border-border text-xs font-semibold text-ink">Owner only</span>
								<span class="px-2.5 py-1 rounded-full bg-surface border border-border text-xs font-semibold text-ink">Temporary signed URL</span>
							</div>
						</article>
					</aside>
				</section>
			</div>
		</div>
	</main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { AlertCircle, ArrowLeft, Camera, CheckCircle, FileText, Shield, User, Stethoscope, Building2, Languages, Award } from 'lucide-vue-next'
import { useAuth } from '@/stores/useAuth'
import { useNotificationStore } from '@/stores/notificationStore'
import doctorService from '@/services/doctorService'
import { resolveDoctorIdentity } from '@/services/doctorDashboardService'

const auth = useAuth()
const notify = useNotificationStore()

const loading = ref(true)
const saving = ref(false)
const licenseLoading = ref(false)
const fetchError = ref('')
const formError = ref('')
const formSuccess = ref(false)
const licenseError = ref('')
const doctor = ref(null)
const hospitals = ref([])
const specializations = ref([])
const licenseInput = ref(null)
const profilePicInput = ref(null)
const hospitalQuery = ref('')

const form = reactive({
	firstName: '',
	lastName: '',
	phone: '',
	bio: '',
	yearsOfExperience: null,
	qualificationsText: '',
	languagesText: '',
	hospitalIds: [],
})

const InfoRow = {
	props: { label: String, value: String, mono: Boolean },
	template: `
		<div>
			<p class="text-xs text-muted mb-0.5">{{ label }}</p>
			<p :class="['text-sm font-medium text-ink break-all', mono ? 'font-mono text-xs' : '']">{{ value || '—' }}</p>
		</div>
	`,
}

const fullName = computed(() => {
	const first = doctor.value?.firstName || ''
	const last = doctor.value?.lastName || ''
	return `${first} ${last}`.trim() || doctor.value?.email || 'Doctor'
})

const initials = computed(() => {
	const first = doctor.value?.firstName?.[0] || ''
	const last = doctor.value?.lastName?.[0] || ''
	return `${first}${last}`.toUpperCase() || 'D'
})

const statusClass = computed(() => {
	const status = String(doctor.value?.status || '').toUpperCase()
	if (status === 'ACTIVE') return 'bg-green-50 text-green-700'
	if (status.includes('PENDING')) return 'bg-amber-50 text-amber-700'
	if (status === 'SUSPENDED') return 'bg-red-50 text-red-700'
	return 'bg-primary/10 text-primary'
})

const specializationName = computed(() => {
	return specializations.value.find((item) => item.id === doctor.value?.specialization)?.name || ''
})

const selectedHospitals = computed(() => {
	const selected = new Set(form.hospitalIds)
	return hospitals.value.filter((hospital) => selected.has(hospital.id))
})

const filteredHospitals = computed(() => {
	const query = hospitalQuery.value.trim().toLowerCase()
	if (!query) return hospitals.value

	return hospitals.value.filter((hospital) => {
		const name = String(hospital?.name || '').toLowerCase()
		const city = String(hospital?.city || '').toLowerCase()
		return name.includes(query) || city.includes(query)
	})
})

const languageList = computed(() => splitCsv(form.languagesText))
const qualificationList = computed(() => splitCsv(form.qualificationsText))

const quickStats = computed(() => [
	{ label: 'Specialization', value: specializationName.value || doctor.value?.specialization || '—', icon: Stethoscope },
	{ label: 'Experience', value: doctor.value?.yearsOfExperience != null ? `${doctor.value.yearsOfExperience} years` : '—', icon: Award },
	{ label: 'Hospitals', value: form.hospitalIds.length ? `${form.hospitalIds.length} selected` : '—', icon: Building2 },
	{ label: 'Languages', value: languageList.value.length ? `${languageList.value.length} listed` : '—', icon: Languages },
])

const formattedCreatedAt = computed(() => {
	if (!doctor.value?.createdAt) return null
	return new Date(doctor.value.createdAt).toLocaleDateString('en-GB', {
		day: 'numeric',
		month: 'short',
		year: 'numeric',
	})
})

function splitCsv(value) {
	return String(value || '')
		.split(',')
		.map((item) => item.trim())
		.filter(Boolean)
}

function resetForm() {
	if (!doctor.value) return
	hydrateForm(doctor.value)
	formError.value = ''
	formSuccess.value = false
}

function toggleHospital(hospitalId) {
	if (!hospitalId) return
	const current = new Set(form.hospitalIds)
	if (current.has(hospitalId)) {
		current.delete(hospitalId)
	} else {
		current.add(hospitalId)
	}
	form.hospitalIds = [...current]
}

function hydrateForm(profile) {
	form.firstName = profile.firstName || ''
	form.lastName = profile.lastName || ''
	form.phone = profile.phone || ''
	form.bio = profile.bio || ''
	form.yearsOfExperience = profile.yearsOfExperience ?? null
	form.qualificationsText = Array.isArray(profile.qualifications) ? profile.qualifications.join(', ') : ''
	form.languagesText = Array.isArray(profile.languages) ? profile.languages.join(', ') : ''
	form.hospitalIds = Array.isArray(profile.hospitals) ? [...profile.hospitals] : []
}

async function loadProfile() {
	loading.value = true
	fetchError.value = ''
	licenseError.value = ''

	try {
		if (auth.token && !auth.user) {
			await auth.fetchMe()
		}

		if (!auth.token) {
			throw new Error('Please sign in again.')
		}

		const identity = await resolveDoctorIdentity({
			token: auth.token,
			authId: auth.user?.authId,
			email: auth.user?.email,
		})

		if (!identity?.doctorId) {
			throw new Error('We could not find your doctor record yet. If you just registered, please try again after verification.')
		}

		const [profile, hospitalOptions, specializationOptions] = await Promise.all([
			doctorService.getDoctorProfile({ token: auth.token, doctorId: identity.doctorId }),
			doctorService.getHospitals(),
			doctorService.getSpecializations(),
		])

		doctor.value = profile
		hospitals.value = Array.isArray(hospitalOptions) ? hospitalOptions : []
		specializations.value = Array.isArray(specializationOptions) ? specializationOptions : []
		hospitalQuery.value = ''
		hydrateForm(profile)
	} catch (error) {
		fetchError.value = error?.message || 'Unable to load your profile right now.'
	} finally {
		loading.value = false
	}
}

async function saveProfile() {
	formError.value = ''
	formSuccess.value = false

	if (!doctor.value?.doctorId) {
		formError.value = 'Doctor profile is not loaded yet.'
		return
	}

	if (!form.firstName.trim() || !form.lastName.trim()) {
		formError.value = 'First and last name are required.'
		return
	}

	saving.value = true
	try {
		const payload = {
			firstName: form.firstName.trim(),
			lastName: form.lastName.trim(),
			phone: form.phone.trim() || null,
			bio: form.bio.trim() || null,
			yearsOfExperience: form.yearsOfExperience === '' ? null : form.yearsOfExperience,
			qualifications: splitCsv(form.qualificationsText),
			languages: splitCsv(form.languagesText),
			hospitalIds: form.hospitalIds,
		}

		const updated = await doctorService.updateDoctorProfile({
			token: auth.token,
			doctorId: doctor.value.doctorId,
			payload,
		})

		doctor.value = updated
		hydrateForm(updated)
		formSuccess.value = true
		notify.push('Profile updated successfully.', 'success')
	} catch (error) {
		formError.value = error?.message || 'Could not save profile changes.'
	} finally {
		saving.value = false
	}
}

function triggerProfilePicUpload() {
	profilePicInput.value?.click()
}

function onProfilePicChange(event) {
	const file = event.target.files?.[0]
	if (!file) return
	void uploadProfilePicture(file)
}

async function uploadProfilePicture(file) {
	licenseError.value = ''

	const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
	if (!allowedTypes.includes(file.type)) {
		licenseError.value = 'Only image files are allowed for profile photo.'
		return
	}

	if (file.size > 10 * 1024 * 1024) {
		licenseError.value = 'Profile photo must be 10MB or less.'
		return
	}

	try {
		const result = await doctorService.getProfilePicUploadUrl({
			token: auth.token,
			fileName: file.name,
		})

		await doctorService.uploadFileToPresignedUrl({
			uploadUrl: result.uploadUrl,
			file,
		})

		if (result.permanentUrl) {
			doctor.value.profilePicUrl = result.permanentUrl
		}

		notify.push('Profile photo updated.', 'success')
	} catch (error) {
		licenseError.value = error?.message || 'Could not upload profile photo.'
	} finally {
		if (profilePicInput.value) profilePicInput.value.value = ''
	}
}

async function viewLicense() {
	licenseLoading.value = true
	licenseError.value = ''

	try {
		const result = await doctorService.getLicenseSignedUrl({
			token: auth.token,
			doctorId: doctor.value.doctorId,
		})

		if (result?.url) {
			window.open(result.url, '_blank', 'noopener,noreferrer')
			return
		}

		throw new Error('Could not open the license document.')
	} catch (error) {
		licenseError.value = error?.message || 'Could not load license document.'
	} finally {
		licenseLoading.value = false
	}
}

onMounted(() => {
	void loadProfile()
})
</script>
