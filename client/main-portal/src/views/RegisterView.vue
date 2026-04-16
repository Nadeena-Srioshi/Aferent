<template>
  <div class="min-h-screen bg-surface flex">
    <div class="w-full flex overflow-hidden min-h-screen">

      <!-- Left panel -->
      <div
        class="hidden md:flex w-5/12 flex-col justify-between p-12 relative overflow-hidden"
        style="background: #004D51;"
      >
        <div class="absolute -top-14 -right-16 w-56 h-56 rounded-full opacity-20 bg-action"></div>
        <div class="absolute -right-10 bottom-16 w-36 h-36 rounded-full opacity-25 bg-primary"></div>

        <!-- Logo -->
        <RouterLink to="/" class="flex items-center gap-2 relative z-10">
          <div class="w-8 h-8 rounded-lg flex items-center justify-center" style="background:rgba(255,255,255,0.15);">
            <HeartPulse class="w-4 h-4 text-white" aria-hidden="true" />
          </div>
          <span class="text-lg font-medium text-white tracking-tight">Aferent</span>
        </RouterLink>

        <!-- Step indicators -->
        <div class="relative z-10 space-y-5">
          <div
            v-for="(s, i) in steps"
            :key="i"
            class="flex items-start gap-4"
          >
            <!-- Step circle -->
            <div class="flex flex-col items-center">
              <div
                class="w-8 h-8 rounded-full flex items-center justify-center text-xs font-semibold transition-all duration-300 shrink-0"
                :class="[
                  currentStep > i + 1
                    ? 'bg-white text-primary'
                    : currentStep === i + 1
                      ? 'bg-white text-primary ring-4 ring-white/20'
                      : 'text-white/50 border border-white/20'
                ]"
              >
                <component
                  v-if="currentStep > i + 1"
                  :is="CheckIcon"
                  class="w-4 h-4"
                />
                <span v-else>{{ i + 1 }}</span>
              </div>
              <div v-if="i < steps.length - 1" class="w-px h-8 mt-1"
                :class="currentStep > i + 1 ? 'bg-white/60' : 'bg-white/15'"
              ></div>
            </div>

            <div class="pt-1">
              <p class="text-sm font-medium transition-colors" :class="currentStep === i + 1 ? 'text-white' : 'text-white/50'">
                {{ s.title }}
              </p>
              <p class="text-xs mt-0.5" :class="currentStep === i + 1 ? 'text-white/70' : 'text-white/30'">
                {{ s.desc }}
              </p>
            </div>
          </div>
        </div>

        <!-- Tagline -->
        <div class="relative z-10">
          <h2 class="text-xl font-medium text-white leading-snug mb-2">Care that fits your life.</h2>
          <p class="text-xs leading-relaxed" style="color:rgba(255,255,255,0.55);">
            Book appointments, consult doctors, and manage your health — all in one place.
          </p>
        </div>
      </div>

      <!-- Right panel -->
      <div class="flex-1 bg-white flex items-center justify-center p-6 md:p-10 min-h-screen">
        <div class="w-full max-w-md">

          <!-- Mobile logo -->
          <RouterLink to="/" class="flex md:hidden items-center gap-2 mb-8">
            <div class="w-7 h-7 rounded-lg flex items-center justify-center bg-primary">
              <HeartPulse class="w-3.5 h-3.5 text-white" aria-hidden="true" />
            </div>
            <span class="text-base font-medium text-ink tracking-tight">Aferent</span>
          </RouterLink>

          <!-- Mobile step dots -->
          <div class="flex md:hidden items-center gap-2 mb-6">
            <div
              v-for="i in 3" :key="i"
              class="h-1.5 rounded-full transition-all duration-300"
              :class="[
                i === currentStep ? 'bg-primary w-6' : i < currentStep ? 'bg-primary/40 w-3' : 'bg-border w-3'
              ]"
            ></div>
          </div>

          <!-- ── STEP 1: Credentials ── -->
          <transition name="fade-slide" mode="out-in">
            <div v-if="currentStep === 1" key="step1">
              <div class="mb-7">
                <h1 class="text-xl font-semibold text-ink mb-1">Create your account</h1>
                <p class="text-sm text-muted">Step 1 of 3 — set your login details.</p>
              </div>

              <div class="space-y-4">
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Email</label>
                  <div class="relative">
                    <MailIcon class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted/60" />
                    <input
                      v-model="creds.email"
                      type="email"
                      autocomplete="email"
                      placeholder="you@example.com"
                      class="w-full pl-10 pr-4 py-2.5 rounded-xl border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                      :class="fieldErr.email ? 'border-danger' : 'border-border'"
                    />
                  </div>
                  <p v-if="fieldErr.email" class="text-xs text-danger mt-1 flex items-center gap-1">
                    <AlertCircleIcon class="w-3 h-3" />{{ fieldErr.email }}
                  </p>
                </div>

                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Password</label>
                  <div class="relative">
                    <LockIcon class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted/60" />
                    <input
                      v-model="creds.password"
                      :type="showPassword ? 'text' : 'password'"
                      autocomplete="new-password"
                      placeholder="At least 8 characters"
                      class="w-full pl-10 pr-10 py-2.5 rounded-xl border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                      :class="fieldErr.password ? 'border-danger' : 'border-border'"
                    />
                    <button type="button" @click="showPassword = !showPassword"
                      class="absolute right-3.5 top-1/2 -translate-y-1/2 text-muted/60 hover:text-muted transition-colors">
                      <EyeOffIcon v-if="showPassword" class="w-4 h-4" />
                      <EyeIcon v-else class="w-4 h-4" />
                    </button>
                  </div>
                  <!-- Password strength -->
                  <div v-if="creds.password" class="mt-2 space-y-1">
                    <div class="flex gap-1">
                      <div v-for="i in 4" :key="i"
                        class="h-1 flex-1 rounded-full transition-all duration-300"
                        :class="i <= passwordStrength.score ? passwordStrength.color : 'bg-border'"
                      ></div>
                    </div>
                    <p class="text-xs" :class="passwordStrength.textColor">{{ passwordStrength.label }}</p>
                  </div>
                  <p v-if="fieldErr.password" class="text-xs text-danger mt-1 flex items-center gap-1">
                    <AlertCircleIcon class="w-3 h-3" />{{ fieldErr.password }}
                  </p>
                </div>

                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Confirm Password</label>
                  <div class="relative">
                    <LockIcon class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted/60" />
                    <input
                      v-model="creds.confirmPassword"
                      :type="showPassword ? 'text' : 'password'"
                      autocomplete="new-password"
                      placeholder="Repeat your password"
                      class="w-full pl-10 pr-4 py-2.5 rounded-xl border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                      :class="fieldErr.confirmPassword ? 'border-danger' : 'border-border'"
                    />
                  </div>
                  <p v-if="fieldErr.confirmPassword" class="text-xs text-danger mt-1 flex items-center gap-1">
                    <AlertCircleIcon class="w-3 h-3" />{{ fieldErr.confirmPassword }}
                  </p>
                </div>

                <!-- Global error -->
                <div v-if="globalError" class="flex items-start gap-2.5 px-4 py-3 rounded-xl bg-red-50 border border-red-100">
                  <AlertCircleIcon class="w-4 h-4 text-danger shrink-0 mt-0.5" />
                  <p class="text-xs text-danger leading-relaxed">{{ globalError }}</p>
                </div>

                <button
                  @click="submitStep1"
                  :disabled="loading"
                  class="w-full py-3 text-white text-sm font-semibold rounded-xl transition-all disabled:opacity-60 disabled:cursor-not-allowed bg-primary hover:bg-action active:scale-[0.99]"
                >
                  <span v-if="loading" class="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2 align-middle"></span>
                  {{ loading ? 'Creating account…' : 'Continue' }}
                </button>
              </div>

              <p class="mt-5 text-center text-xs text-muted">
                Already have an account?
                <RouterLink to="/login" class="font-semibold text-primary hover:underline">Sign in</RouterLink>
              </p>
            </div>
          </transition>

          <!-- ── STEP 2: Basic Profile (required-ish) ── -->
          <transition name="fade-slide" mode="out-in">
            <div v-if="currentStep === 2" key="step2">
              <div class="mb-7">
                <h1 class="text-xl font-semibold text-ink mb-1">Tell us about yourself</h1>
                <p class="text-sm text-muted">Step 2 of 3 — basic information for your profile.</p>
              </div>

              <div class="space-y-4">
                <div class="grid grid-cols-2 gap-3">
                  <div>
                    <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">First Name <span class="text-danger">*</span></label>
                    <input
                      v-model="profile.firstName"
                      type="text"
                      placeholder="John"
                      class="w-full px-3.5 py-2.5 rounded-xl border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                      :class="fieldErr.firstName ? 'border-danger' : 'border-border'"
                    />
                    <p v-if="fieldErr.firstName" class="text-xs text-danger mt-1 flex items-center gap-1">
                      <AlertCircleIcon class="w-3 h-3" />{{ fieldErr.firstName }}
                    </p>
                  </div>
                  <div>
                    <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Last Name <span class="text-danger">*</span></label>
                    <input
                      v-model="profile.lastName"
                      type="text"
                      placeholder="Doe"
                      class="w-full px-3.5 py-2.5 rounded-xl border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                      :class="fieldErr.lastName ? 'border-danger' : 'border-border'"
                    />
                    <p v-if="fieldErr.lastName" class="text-xs text-danger mt-1 flex items-center gap-1">
                      <AlertCircleIcon class="w-3 h-3" />{{ fieldErr.lastName }}
                    </p>
                  </div>
                </div>

                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Phone <span class="text-danger">*</span></label>
                  <div class="relative">
                    <PhoneIcon class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted/60" />
                    <input
                      v-model="profile.phone"
                      type="tel"
                      placeholder="+1-555-0000"
                      class="w-full pl-10 pr-4 py-2.5 rounded-xl border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                      :class="fieldErr.phone ? 'border-danger' : 'border-border'"
                    />
                  </div>
                  <p v-if="fieldErr.phone" class="text-xs text-danger mt-1 flex items-center gap-1">
                    <AlertCircleIcon class="w-3 h-3" />{{ fieldErr.phone }}
                  </p>
                </div>

                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Date of Birth <span class="text-danger">*</span></label>
                  <div class="relative">
                    <CalendarIcon class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted/60" />
                    <input
                      v-model="profile.dateOfBirth"
                      type="date"
                      :max="maxDobDate"
                      class="w-full pl-10 pr-4 py-2.5 rounded-xl border text-sm text-ink focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                      :class="fieldErr.dateOfBirth ? 'border-danger' : 'border-border'"
                    />
                  </div>
                  <p v-if="fieldErr.dateOfBirth" class="text-xs text-danger mt-1 flex items-center gap-1">
                    <AlertCircleIcon class="w-3 h-3" />{{ fieldErr.dateOfBirth }}
                  </p>
                </div>

                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Gender <span class="text-danger">*</span></label>
                  <div class="grid grid-cols-3 gap-2">
                    <button
                      v-for="g in genderOptions" :key="g.value"
                      type="button"
                      @click="profile.gender = g.value"
                      class="py-2.5 rounded-xl border text-sm font-medium transition-all"
                      :class="profile.gender === g.value
                        ? 'border-primary bg-primary/5 text-primary'
                        : 'border-border text-muted bg-surface hover:border-primary/30'"
                    >
                      {{ g.label }}
                    </button>
                  </div>
                  <p v-if="fieldErr.gender" class="text-xs text-danger mt-1 flex items-center gap-1">
                    <AlertCircleIcon class="w-3 h-3" />{{ fieldErr.gender }}
                  </p>
                </div>

                <!-- Global error -->
                <div v-if="globalError" class="flex items-start gap-2.5 px-4 py-3 rounded-xl bg-red-50 border border-red-100">
                  <AlertCircleIcon class="w-4 h-4 text-danger shrink-0 mt-0.5" />
                  <p class="text-xs text-danger leading-relaxed">{{ globalError }}</p>
                </div>

                <div class="flex gap-3 pt-1">
                  <button
                    @click="currentStep = 1"
                    class="flex-1 py-3 text-sm font-semibold rounded-xl border border-border text-muted hover:bg-surface transition-colors"
                  >
                    Back
                  </button>
                  <button
                    @click="submitStep2"
                    :disabled="loading"
                    class="flex-[2] py-3 text-white text-sm font-semibold rounded-xl transition-all disabled:opacity-60 disabled:cursor-not-allowed bg-primary hover:bg-action active:scale-[0.99]"
                  >
                    <span v-if="loading" class="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2 align-middle"></span>
                    {{ loading ? 'Saving…' : 'Continue' }}
                  </button>
                </div>
              </div>
            </div>
          </transition>

          <!-- ── STEP 3: Optional extras ── -->
          <transition name="fade-slide" mode="out-in">
            <div v-if="currentStep === 3" key="step3">
              <div class="mb-7">
                <div class="flex items-start justify-between">
                  <div>
                    <h1 class="text-xl font-semibold text-ink mb-1">Almost done!</h1>
                    <p class="text-sm text-muted">Step 3 of 3 — optional health details.</p>
                  </div>
                  <button
                    @click="skipAndFinish"
                    :disabled="loading"
                    class="text-xs text-muted hover:text-ink underline underline-offset-2 transition-colors shrink-0 mt-1"
                  >Skip</button>
                </div>
              </div>

              <div class="space-y-4">
                <!-- Profile image upload placeholder -->
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-2">Profile Photo <span class="text-muted font-normal normal-case">(optional)</span></label>
                  <div class="flex items-center gap-4">
                    <div class="relative w-16 h-16 shrink-0">
                      <div class="w-16 h-16 rounded-2xl bg-primary/10 flex items-center justify-center overflow-hidden border-2 border-border">
                        <img v-if="avatarPreview" :src="avatarPreview" alt="Preview" class="w-full h-full object-cover" />
                        <UserIcon v-else class="w-8 h-8 text-primary/40" />
                      </div>
                    </div>
                    <div>
                      <button
                        type="button"
                        class="px-4 py-2 text-xs font-semibold border border-border rounded-lg text-muted bg-surface hover:border-primary/40 hover:text-primary transition-colors flex items-center gap-1.5"
                        @click="triggerAvatarUpload"
                      >
                        <CameraIcon class="w-3.5 h-3.5" />
                        Upload photo
                      </button>
                      <p class="text-xs text-muted/60 mt-1">JPG, PNG up to 5MB</p>
                      <input ref="avatarInput" type="file" accept="image/*" class="hidden" @change="onAvatarChange" />
                    </div>
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-3">
                  <div>
                    <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Blood Group</label>
                    <select
                      v-model="profile.bloodGroup"
                      class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors"
                    >
                      <option value="">Select</option>
                      <option v-for="bg in bloodGroups" :key="bg" :value="bg">{{ bg }}</option>
                    </select>
                  </div>
                  <div>
                    <!-- spacer or future field -->
                  </div>
                </div>

                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Street Address</label>
                  <div class="relative">
                    <MapPinIcon class="absolute left-3.5 top-3 w-4 h-4 text-muted/60" />
                    <input
                      v-model="profile.address.street"
                      type="text"
                      placeholder="221B Baker Street"
                      class="w-full pl-10 pr-4 py-2.5 rounded-xl border border-border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                    />
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-3">
                  <div>
                    <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">City</label>
                    <input
                      v-model="profile.address.city"
                      type="text"
                      placeholder="London"
                      class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                    />
                  </div>
                  <div>
                    <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Country</label>
                    <input
                      v-model="profile.address.country"
                      type="text"
                      placeholder="UK"
                      class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                    />
                  </div>
                </div>

                <!-- Global error -->
                <div v-if="globalError" class="flex items-start gap-2.5 px-4 py-3 rounded-xl bg-red-50 border border-red-100">
                  <AlertCircleIcon class="w-4 h-4 text-danger shrink-0 mt-0.5" />
                  <p class="text-xs text-danger leading-relaxed">{{ globalError }}</p>
                </div>

                <div class="flex gap-3 pt-1">
                  <button
                    @click="currentStep = 2"
                    class="flex-1 py-3 text-sm font-semibold rounded-xl border border-border text-muted hover:bg-surface transition-colors"
                  >
                    Back
                  </button>
                  <button
                    @click="submitStep3"
                    :disabled="loading"
                    class="flex-[2] py-3 text-white text-sm font-semibold rounded-xl transition-all disabled:opacity-60 disabled:cursor-not-allowed bg-primary hover:bg-action active:scale-[0.99]"
                  >
                    <span v-if="loading" class="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2 align-middle"></span>
                    {{ loading ? 'Finishing…' : 'Complete Setup' }}
                  </button>
                </div>
              </div>
            </div>
          </transition>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useAuth } from '@/stores/useAuth'
import authService from '@/services/authService'
import patientService from '@/services/patientService'
import {
  Mail as MailIcon,
  Lock as LockIcon,
  Eye as EyeIcon,
  EyeOff as EyeOffIcon,
  AlertCircle as AlertCircleIcon,
  Phone as PhoneIcon,
  Calendar as CalendarIcon,
  MapPin as MapPinIcon,
  User as UserIcon,
  Camera as CameraIcon,
  Check as CheckIcon,
} from 'lucide-vue-next'

// ── Use auth store or adapt to your setup ──
// import { useAuthStore } from '@/stores/auth'
// const authStore = useAuthStore()

const router = useRouter()
const authStore = useAuth()

// ── State ──
const currentStep = ref(1)
const loading = ref(false)
const globalError = ref(null)
const fieldErr = reactive({})
const showPassword = ref(false)
const avatarInput = ref(null)
const avatarPreview = ref(null)

// Persisted across steps
const authResult = ref(null) // { accessToken, authId, role }

const creds = reactive({
  email: '',
  password: '',
  confirmPassword: '',
})

const profile = reactive({
  firstName: '',
  lastName: '',
  phone: '',
  dateOfBirth: '',
  gender: '',
  bloodGroup: '',
  address: {
    street: '',
    city: '',
    country: '',
  },
})

// ── Static data ──
const steps = [
  { title: 'Account credentials', desc: 'Email and password' },
  { title: 'Basic information', desc: 'Name, phone, date of birth' },
  { title: 'Health details', desc: 'Optional — skip anytime' },
]

const genderOptions = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
]

const bloodGroups = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']

const maxDobDate = new Date(Date.now() - 365 * 24 * 60 * 60 * 1000 * 1)
  .toISOString().split('T')[0]

// ── Password strength ──
const passwordStrength = computed(() => {
  const p = creds.password
  let score = 0
  if (p.length >= 8) score++
  if (/[A-Z]/.test(p)) score++
  if (/[0-9]/.test(p)) score++
  if (/[^A-Za-z0-9]/.test(p)) score++

  const map = [
    { label: 'Too weak', color: 'bg-danger', textColor: 'text-danger' },
    { label: 'Weak', color: 'bg-warning', textColor: 'text-warning' },
    { label: 'Fair', color: 'bg-warning', textColor: 'text-warning' },
    { label: 'Strong', color: 'bg-success', textColor: 'text-success' },
    { label: 'Very strong', color: 'bg-success', textColor: 'text-success' },
  ]
  return { score, ...map[score] }
})

// ── Helpers ──
function clearErrors() {
  globalError.value = null
  Object.keys(fieldErr).forEach(k => delete fieldErr[k])
}

function setAuthToken(token) {
  authStore.saveSession(token, authResult.value || null)
}

// ── Avatar handling (upload logic deferred) ──
function triggerAvatarUpload() {
  avatarInput.value?.click()
}

function onAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (ev) => { avatarPreview.value = ev.target.result }
  reader.readAsDataURL(file)
  // TODO: upload logic will be added later
}

// ── STEP 1: Register ──
async function submitStep1() {
  clearErrors()

  let valid = true
  if (!creds.email) { fieldErr.email = 'Email is required.'; valid = false }
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(creds.email)) { fieldErr.email = 'Enter a valid email address.'; valid = false }
  if (!creds.password) { fieldErr.password = 'Password is required.'; valid = false }
  else if (creds.password.length < 8) { fieldErr.password = 'Password must be at least 8 characters.'; valid = false }
  if (!creds.confirmPassword) { fieldErr.confirmPassword = 'Please confirm your password.'; valid = false }
  else if (creds.password !== creds.confirmPassword) { fieldErr.confirmPassword = 'Passwords do not match.'; valid = false }
  if (!valid) return

  loading.value = true
  try {
    const data = await authService.register({
      email: creds.email,
      password: creds.password,
      role: 'PATIENT',
    })

    // Auto-login: store token from registration response
    authResult.value = data
    setAuthToken(data.accessToken)
    currentStep.value = 2

  } catch (e) {
      const status = e?.status
      if (status === 409) {
        fieldErr.email = 'An account with this email already exists.'
      } else if (status === 400) {
        globalError.value = e?.message || 'Please check your details and try again.'
      } else {
        globalError.value = e?.message || 'Registration failed. Please try again.'
      }
  } finally {
    loading.value = false
  }
}

// ── STEP 2: Basic profile (required fields) ──
async function submitStep2() {
  clearErrors()

  let valid = true
  if (!profile.firstName.trim()) { fieldErr.firstName = 'First name is required.'; valid = false }
  if (!profile.lastName.trim()) { fieldErr.lastName = 'Last name is required.'; valid = false }
  if (!profile.phone.trim()) { fieldErr.phone = 'Phone number is required.'; valid = false }
  if (!profile.dateOfBirth) { fieldErr.dateOfBirth = 'Date of birth is required.'; valid = false }
  if (!profile.gender) { fieldErr.gender = 'Please select a gender.'; valid = false }
  if (!valid) return

  // Step 2 is kept in local state; we persist profile after step 3/skip.
  currentStep.value = 3
}

// ── Resolve patientId from /patients/me then PUT /patients/{patientId} ──
async function saveProfile(extraFields = {}) {
  const payload = {
    firstName: profile.firstName,
    lastName: profile.lastName,
    phone: profile.phone,
    dateOfBirth: profile.dateOfBirth,
    gender: profile.gender,
    ...extraFields,
  }

  if (!authResult.value?.accessToken) throw new Error('Access token not available.')

  // Keep auth store hydrated so downstream profile calls always use a current token.
  authStore.saveSession(authResult.value.accessToken, authResult.value)

  const patientId = await patientService.resolvePatientId({ token: authResult.value.accessToken })
  if (!patientId) throw new Error('Patient ID not available yet. Please try again in a moment.')
  localStorage.setItem('patientId', patientId)

  return patientService.updateProfile({
    patientId,
    token: authResult.value.accessToken,
    payload,
  })
}

// ── STEP 3: Optional extras, then finish ──
async function submitStep3() {
  clearErrors()
  loading.value = true
  try {
    const extra = {}
    if (profile.bloodGroup) extra.bloodGroup = profile.bloodGroup
    const addr = profile.address
    if (addr.street || addr.city || addr.country) {
      extra.address = {
        street: addr.street || undefined,
        city: addr.city || undefined,
        country: addr.country || undefined,
      }
    }
    await saveProfile(extra)
    router.push('/')
  } catch (e) {
    globalError.value = e.message || 'Could not save profile. You can update it later.'
  } finally {
    loading.value = false
  }
}

async function skipAndFinish() {
  clearErrors()
  loading.value = true
  try {
    await saveProfile()
    router.push('/')
  } catch (e) {
    // Profile save failed — still navigate, user can complete profile later
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(16px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-16px);
}
</style>