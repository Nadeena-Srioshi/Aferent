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
            <Stethoscope class="w-4 h-4 text-white" aria-hidden="true" />
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
                <Check v-if="currentStep > i + 1" class="w-4 h-4" />
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
          <h2 class="text-xl font-medium text-white leading-snug mb-2">Grow your practice with Aferent.</h2>
          <p class="text-xs leading-relaxed" style="color:rgba(255,255,255,0.55);">
            Join verified specialists and connect with patients who need your expertise — all in one platform.
          </p>
        </div>
      </div>

      <!-- Right panel -->
      <div class="flex-1 bg-white flex items-center justify-center p-6 md:p-10 min-h-screen">
        <div class="w-full max-w-md">

          <!-- Mobile logo -->
          <RouterLink to="/" class="flex md:hidden items-center gap-2 mb-8">
            <div class="w-7 h-7 rounded-lg flex items-center justify-center bg-primary">
              <Stethoscope class="w-3.5 h-3.5 text-white" aria-hidden="true" />
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
                <h1 class="text-xl font-semibold text-ink mb-1">Create your doctor account</h1>
                <p class="text-sm text-muted">Step 1 of 3 — set your login credentials.</p>
              </div>

              <div class="space-y-4">
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Email</label>
                  <div class="relative">
                    <Mail class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted/60" />
                    <input
                      v-model="creds.email"
                      type="email"
                      autocomplete="email"
                      placeholder="you@clinic.com"
                      class="w-full pl-10 pr-4 py-2.5 rounded-xl border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                      :class="fieldErr.email ? 'border-danger' : 'border-border'"
                    />
                  </div>
                  <p v-if="fieldErr.email" class="text-xs text-danger mt-1 flex items-center gap-1">
                    <AlertCircle class="w-3 h-3" />{{ fieldErr.email }}
                  </p>
                </div>

                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Password</label>
                  <div class="relative">
                    <Lock class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted/60" />
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
                      <EyeOff v-if="showPassword" class="w-4 h-4" />
                      <Eye v-else class="w-4 h-4" />
                    </button>
                  </div>
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
                    <AlertCircle class="w-3 h-3" />{{ fieldErr.password }}
                  </p>
                </div>

                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Confirm Password</label>
                  <div class="relative">
                    <Lock class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted/60" />
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
                    <AlertCircle class="w-3 h-3" />{{ fieldErr.confirmPassword }}
                  </p>
                </div>

                <div v-if="globalError" class="flex items-start gap-2.5 px-4 py-3 rounded-xl bg-red-50 border border-red-100">
                  <AlertCircle class="w-4 h-4 text-danger shrink-0 mt-0.5" />
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

          <!-- ── STEP 2: Doctor Profile ── -->
          <transition name="fade-slide" mode="out-in">
            <div v-if="currentStep === 2" key="step2">
              <div class="mb-7">
                <h1 class="text-xl font-semibold text-ink mb-1">Your professional profile</h1>
                <p class="text-sm text-muted">Step 2 of 3 — your qualifications and practice details.</p>
              </div>

              <div class="space-y-4">
                <div class="grid grid-cols-2 gap-3">
                  <div>
                    <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">First Name <span class="text-danger">*</span></label>
                    <input
                      v-model="doctorProfile.firstName"
                      type="text"
                      placeholder="Jane"
                      class="w-full px-3.5 py-2.5 rounded-xl border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                      :class="fieldErr.firstName ? 'border-danger' : 'border-border'"
                    />
                    <p v-if="fieldErr.firstName" class="text-xs text-danger mt-1 flex items-center gap-1">
                      <AlertCircle class="w-3 h-3" />{{ fieldErr.firstName }}
                    </p>
                  </div>
                  <div>
                    <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Last Name <span class="text-danger">*</span></label>
                    <input
                      v-model="doctorProfile.lastName"
                      type="text"
                      placeholder="Smith"
                      class="w-full px-3.5 py-2.5 rounded-xl border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                      :class="fieldErr.lastName ? 'border-danger' : 'border-border'"
                    />
                    <p v-if="fieldErr.lastName" class="text-xs text-danger mt-1 flex items-center gap-1">
                      <AlertCircle class="w-3 h-3" />{{ fieldErr.lastName }}
                    </p>
                  </div>
                </div>

                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Phone <span class="text-danger">*</span></label>
                  <div class="relative">
                    <Phone class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted/60" />
                    <input
                      v-model="doctorProfile.phone"
                      type="tel"
                      placeholder="+1-555-0000"
                      class="w-full pl-10 pr-4 py-2.5 rounded-xl border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                      :class="fieldErr.phone ? 'border-danger' : 'border-border'"
                    />
                  </div>
                  <p v-if="fieldErr.phone" class="text-xs text-danger mt-1 flex items-center gap-1">
                    <AlertCircle class="w-3 h-3" />{{ fieldErr.phone }}
                  </p>
                </div>

                <!-- Specialization -->
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Specialization <span class="text-danger">*</span></label>
                  <div class="relative">
                    <Stethoscope class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted/60" />
                    <select
                      v-model="doctorProfile.specialization"
                      class="w-full pl-10 pr-4 py-2.5 rounded-xl border text-sm text-ink bg-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors appearance-none"
                      :class="fieldErr.specialization ? 'border-danger' : 'border-border'"
                    >
                      <option value="" disabled>
                        {{ loadingSpecializations ? 'Loading…' : 'Select specialization' }}
                      </option>
                      <option
                        v-for="s in specializations"
                        :key="s.id"
                        :value="s.id"
                      >{{ s.name }}</option>
                    </select>
                    <ChevronDown class="absolute right-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted/60 pointer-events-none" />
                  </div>
                  <p v-if="fieldErr.specialization" class="text-xs text-danger mt-1 flex items-center gap-1">
                    <AlertCircle class="w-3 h-3" />{{ fieldErr.specialization }}
                  </p>
                </div>

                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">License Number <span class="text-danger">*</span></label>
                  <div class="relative">
                    <BadgeCheck class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted/60" />
                    <input
                      v-model="doctorProfile.licenseNumber"
                      type="text"
                      placeholder="e.g. MD-123456"
                      class="w-full pl-10 pr-4 py-2.5 rounded-xl border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                      :class="fieldErr.licenseNumber ? 'border-danger' : 'border-border'"
                    />
                  </div>
                  <p v-if="fieldErr.licenseNumber" class="text-xs text-danger mt-1 flex items-center gap-1">
                    <AlertCircle class="w-3 h-3" />{{ fieldErr.licenseNumber }}
                  </p>
                </div>

                <div class="grid grid-cols-2 gap-3">
                  <div>
                    <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Video Fee (15 min) <span class="text-danger">*</span></label>
                    <div class="relative">
                      <DollarSign class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted/60" />
                      <input
                        v-model.number="doctorProfile.consultationFeeVideo"
                        type="number"
                        min="0"
                        step="0.01"
                        placeholder="e.g. 3500"
                        class="w-full pl-10 pr-4 py-2.5 rounded-xl border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                        :class="fieldErr.consultationFeeVideo ? 'border-danger' : 'border-border'"
                      />
                    </div>
                    <p v-if="fieldErr.consultationFeeVideo" class="text-xs text-danger mt-1 flex items-center gap-1">
                      <AlertCircle class="w-3 h-3" />{{ fieldErr.consultationFeeVideo }}
                    </p>
                  </div>

                  <div>
                    <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Physical Fee (15 min) <span class="text-danger">*</span></label>
                    <div class="relative">
                      <DollarSign class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted/60" />
                      <input
                        v-model.number="doctorProfile.consultationFeePhysical"
                        type="number"
                        min="0"
                        step="0.01"
                        placeholder="e.g. 4500"
                        class="w-full pl-10 pr-4 py-2.5 rounded-xl border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                        :class="fieldErr.consultationFeePhysical ? 'border-danger' : 'border-border'"
                      />
                    </div>
                    <p v-if="fieldErr.consultationFeePhysical" class="text-xs text-danger mt-1 flex items-center gap-1">
                      <AlertCircle class="w-3 h-3" />{{ fieldErr.consultationFeePhysical }}
                    </p>
                  </div>
                </div>

                <p class="text-xs text-muted -mt-1">
                  Set your charges per <strong>15-minute appointment</strong> for both consultation types.
                </p>

                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Years of Experience <span class="text-danger">*</span></label>
                  <div class="relative">
                    <BriefcaseMedical class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted/60" />
                    <input
                      v-model.number="doctorProfile.yearsOfExperience"
                      type="number"
                      min="0"
                      max="60"
                      placeholder="e.g. 10"
                      class="w-full pl-10 pr-4 py-2.5 rounded-xl border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface"
                      :class="fieldErr.yearsOfExperience ? 'border-danger' : 'border-border'"
                    />
                  </div>
                  <p v-if="fieldErr.yearsOfExperience" class="text-xs text-danger mt-1 flex items-center gap-1">
                    <AlertCircle class="w-3 h-3" />{{ fieldErr.yearsOfExperience }}
                  </p>
                </div>

                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Qualifications <span class="text-danger">*</span></label>
                  <textarea
                    v-model="doctorProfile.qualifications"
                    rows="3"
                    placeholder="e.g. MBBS, MD (Cardiology), Fellowship — Johns Hopkins"
                    class="w-full px-3.5 py-2.5 rounded-xl border text-sm text-ink placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors bg-surface resize-none"
                    :class="fieldErr.qualifications ? 'border-danger' : 'border-border'"
                  ></textarea>
                  <p v-if="fieldErr.qualifications" class="text-xs text-danger mt-1 flex items-center gap-1">
                    <AlertCircle class="w-3 h-3" />{{ fieldErr.qualifications }}
                  </p>
                </div>

                <div v-if="globalError" class="flex items-start gap-2.5 px-4 py-3 rounded-xl bg-red-50 border border-red-100">
                  <AlertCircle class="w-4 h-4 text-danger shrink-0 mt-0.5" />
                  <p class="text-xs text-danger leading-relaxed">{{ globalError }}</p>
                </div>

                <div class="pt-1">
                  <button
                    @click="submitStep2"
                    :disabled="loading"
                    class="w-full py-3 text-white text-sm font-semibold rounded-xl transition-all disabled:opacity-60 disabled:cursor-not-allowed bg-primary hover:bg-action active:scale-[0.99]"
                  >
                    <span v-if="loading" class="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2 align-middle"></span>
                    {{ loading ? 'Saving…' : 'Continue' }}
                  </button>
                </div>
              </div>
            </div>
          </transition>

          <!-- ── STEP 3: License upload ── -->
          <transition name="fade-slide" mode="out-in">
            <div v-if="currentStep === 3" key="step3">
              <div class="mb-7">
                <h1 class="text-xl font-semibold text-ink mb-1">License upload</h1>
                <p class="text-sm text-muted">Step 3 of 3 — upload your license document to complete registration.</p>
              </div>

              <div class="space-y-5">
                <!-- Drop zone -->
                <div
                  class="relative border-2 border-dashed rounded-2xl p-8 text-center transition-colors cursor-pointer"
                  :class="[
                    dragOver ? 'border-primary bg-primary/5' : 'border-border hover:border-primary/40 bg-surface',
                    licenseFile ? 'border-success/50 bg-success/5' : ''
                  ]"
                  @click="triggerFileInput"
                  @dragover.prevent="dragOver = true"
                  @dragleave="dragOver = false"
                  @drop.prevent="onDrop"
                  role="button"
                  tabindex="0"
                  @keydown.enter="triggerFileInput"
                  :aria-label="licenseFile ? `Selected: ${licenseFile.name}` : 'Click or drag to upload license document'"
                >
                  <input
                    ref="fileInput"
                    type="file"
                    accept=".pdf,.jpg,.jpeg,.png"
                    class="hidden"
                    @change="onFileChange"
                  />
                  <div v-if="!licenseFile">
                    <UploadCloud class="w-10 h-10 text-muted/40 mx-auto mb-3" aria-hidden="true" />
                    <p class="text-sm text-ink font-medium mb-1">Drag & drop your license here</p>
                    <p class="text-xs text-muted">or click to browse</p>
                    <p class="text-xs text-muted/60 mt-2">PDF, JPG, PNG — max 10MB</p>
                  </div>
                  <div v-else class="flex flex-col items-center gap-2">
                    <div class="w-12 h-12 rounded-2xl bg-success/15 flex items-center justify-center">
                      <FileCheck class="w-6 h-6 text-success" aria-hidden="true" />
                    </div>
                    <p class="text-sm font-medium text-ink">{{ licenseFile.name }}</p>
                    <p class="text-xs text-muted">{{ formatFileSize(licenseFile.size) }}</p>
                    <button
                      type="button"
                      @click.stop="removeLicenseFile"
                      class="text-xs text-danger hover:underline mt-1"
                    >Remove</button>
                  </div>
                </div>

                <p v-if="fieldErr.license" class="text-xs text-danger flex items-center gap-1">
                  <AlertCircle class="w-3 h-3" />{{ fieldErr.license }}
                </p>

                <!-- Info note -->
                <div class="flex items-start gap-2.5 px-4 py-3 rounded-xl bg-primary/5 border border-primary/15">
                  <Info class="w-4 h-4 text-primary shrink-0 mt-0.5" aria-hidden="true" />
                  <p class="text-xs text-primary/80 leading-relaxed">
                    Upload a valid PDF, JPG, or PNG file (max 10MB). Your registration is completed after the upload succeeds.
                  </p>
                </div>

                <div v-if="globalError" class="flex items-start gap-2.5 px-4 py-3 rounded-xl bg-red-50 border border-red-100">
                  <AlertCircle class="w-4 h-4 text-danger shrink-0 mt-0.5" />
                  <p class="text-xs text-danger leading-relaxed">{{ globalError }}</p>
                </div>

                <div class="pt-1">
                  <button
                    @click="submitStep3"
                    :disabled="loading"
                    class="w-full py-3 text-white text-sm font-semibold rounded-xl transition-all disabled:opacity-60 disabled:cursor-not-allowed bg-primary hover:bg-action active:scale-[0.99]"
                  >
                    <span v-if="loading" class="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2 align-middle"></span>
                    {{ loading ? 'Uploading…' : 'Complete Registration' }}
                  </button>
                </div>
              </div>
            </div>
          </transition>

          <!-- ── Pending approval modal ── -->
          <Teleport to="body">
            <transition name="modal">
              <div v-if="showPendingApprovalModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
                <div class="absolute inset-0 bg-black/40 backdrop-blur-sm"></div>

                <div class="relative z-10 w-full max-w-md bg-card rounded-2xl shadow-2xl p-6">
                  <div class="flex items-start gap-3">
                    <div class="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
                      <Info class="w-5 h-5 text-primary" aria-hidden="true" />
                    </div>
                    <div>
                      <h3 class="text-base font-semibold text-ink">Registration received</h3>
                      <p class="text-sm text-muted mt-1 leading-relaxed">
                        We received your information and it is now awaiting admin verification.
                        You will receive an email notification once your account is activated.
                      </p>
                    </div>
                  </div>

                  <button
                    type="button"
                    @click="continueAfterPendingApproval"
                    class="mt-5 w-full py-2.5 text-sm font-semibold rounded-xl bg-primary text-white hover:bg-action transition-colors"
                  >
                    Go to Home
                  </button>
                </div>
              </div>
            </transition>
          </Teleport>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useNotificationStore } from '@/stores/notificationStore'
import doctorService from '@/services/doctorService'
import {
  Stethoscope,
  Mail,
  Lock,
  Eye,
  EyeOff,
  AlertCircle,
  Phone,
  Check,
  ChevronDown,
  BadgeCheck,
  BriefcaseMedical,
  DollarSign,
  UploadCloud,
  FileCheck,
  Info,
} from 'lucide-vue-next'

const router = useRouter()
const notify = useNotificationStore()

// ── State ──
const currentStep = ref(1)
const loading = ref(false)
const globalError = ref(null)
const fieldErr = reactive({})
const showPassword = ref(false)
const fileInput = ref(null)
const licenseFile = ref(null)
const dragOver = ref(false)
const showPendingApprovalModal = ref(false)
const MAX_LICENSE_FILE_SIZE = 10 * 1024 * 1024
const ALLOWED_LICENSE_MIME_TYPES = ['application/pdf', 'image/jpeg', 'image/png']
const ALLOWED_LICENSE_EXTENSIONS = ['pdf', 'jpg', 'jpeg', 'png']

// Reference data
const specializations = ref([])
const loadingSpecializations = ref(false)

// Persisted across steps
const authResult = ref(null) // { accessToken, authId }

const creds = reactive({
  email: '',
  password: '',
  confirmPassword: '',
})

const doctorProfile = reactive({
  firstName: '',
  lastName: '',
  phone: '',
  specialization: '',
  consultationFeeVideo: '',
  consultationFeePhysical: '',
  licenseNumber: '',
  yearsOfExperience: '',
  qualifications: '',
})

const selectedSpecialization = computed(() => (
  specializations.value.find((s) => s.id === doctorProfile.specialization) || null
))

const steps = [
  { title: 'Account credentials', desc: 'Email and password' },
  { title: 'Professional profile', desc: 'Specialization, license, experience' },
  { title: 'License document', desc: 'Required for completion' },
]

// ── Load reference data ──
onMounted(async () => {
  loadingSpecializations.value = true
  try {
    specializations.value = await doctorService.getSpecializations({ includeFees: true })
  } catch {
    notify.push('Could not load specializations. Please refresh.', 'warning')
  } finally {
    loadingSpecializations.value = false
  }
})

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

function formatFileSize(bytes) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function triggerFileInput() {
  fileInput.value?.click()
}

function validateLicenseFile(file) {
  if (!file) return 'License document is required.'

  const extension = file.name.includes('.')
    ? file.name.split('.').pop().toLowerCase()
    : ''

  const isAllowedMimeType = !file.type || ALLOWED_LICENSE_MIME_TYPES.includes(file.type)
  const isAllowedExtension = ALLOWED_LICENSE_EXTENSIONS.includes(extension)

  if (!isAllowedMimeType || !isAllowedExtension) {
    return 'Only PDF, JPG, or PNG files are allowed.'
  }

  if (file.size > MAX_LICENSE_FILE_SIZE) {
    return 'File size must be 10MB or less.'
  }

  return null
}

function setLicenseFile(file) {
  const validationError = validateLicenseFile(file)
  if (validationError) {
    fieldErr.license = validationError
    return
  }

  licenseFile.value = file
  delete fieldErr.license
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (file) {
    setLicenseFile(file)
  }
}

function onDrop(e) {
  dragOver.value = false
  const file = e.dataTransfer.files?.[0]
  if (file) {
    setLicenseFile(file)
  }
}

function removeLicenseFile() {
  licenseFile.value = null
  delete fieldErr.license
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

async function continueAfterPendingApproval() {
  showPendingApprovalModal.value = false
  await router.push('/')
}

// ── STEP 1: Auth register as DOCTOR ──
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
    const data = await doctorService.registerDoctorAuth({
      email: creds.email,
      password: creds.password,
    })
    authResult.value = data
    currentStep.value = 2
  } catch (e) {
    if (e?.status === 409) {
      fieldErr.email = 'An account with this email already exists.'
    } else if (e?.status === 400) {
      globalError.value = e?.message || 'Please check your details and try again.'
    } else {
      globalError.value = e?.message || 'Registration failed. Please try again.'
    }
  } finally {
    loading.value = false
  }
}

// ── STEP 2: Complete doctor profile ──
async function submitStep2() {
  clearErrors()
  let valid = true
  if (!doctorProfile.firstName.trim()) { fieldErr.firstName = 'First name is required.'; valid = false }
  if (!doctorProfile.lastName.trim()) { fieldErr.lastName = 'Last name is required.'; valid = false }
  if (!doctorProfile.phone.trim()) { fieldErr.phone = 'Phone number is required.'; valid = false }
  if (!doctorProfile.specialization) { fieldErr.specialization = 'Please select a specialization.'; valid = false }
  if (!doctorProfile.licenseNumber.trim()) { fieldErr.licenseNumber = 'License number is required.'; valid = false }
  if (doctorProfile.yearsOfExperience === '' || doctorProfile.yearsOfExperience === null) {
    fieldErr.yearsOfExperience = 'Years of experience is required.'; valid = false
  }
  if (!doctorProfile.qualifications.trim()) { fieldErr.qualifications = 'Qualifications are required.'; valid = false }

  const videoFee = Number(doctorProfile.consultationFeeVideo)
  const physicalFee = Number(doctorProfile.consultationFeePhysical)

  if (!Number.isFinite(videoFee) || videoFee <= 0) {
    fieldErr.consultationFeeVideo = 'Video fee for a 15-minute appointment is required.'
    valid = false
  }

  if (!Number.isFinite(physicalFee) || physicalFee <= 0) {
    fieldErr.consultationFeePhysical = 'Physical fee for a 15-minute appointment is required.'
    valid = false
  }

  if (
    selectedSpecialization.value?.maxVideoConsultationFee != null
    && Number.isFinite(videoFee)
    && videoFee > selectedSpecialization.value.maxVideoConsultationFee
  ) {
    fieldErr.consultationFeeVideo = `Video fee cannot exceed LKR ${selectedSpecialization.value.maxVideoConsultationFee} for this specialization.`
    valid = false
  }

  if (
    selectedSpecialization.value?.maxPhysicalConsultationFee != null
    && Number.isFinite(physicalFee)
    && physicalFee > selectedSpecialization.value.maxPhysicalConsultationFee
  ) {
    fieldErr.consultationFeePhysical = `Physical fee cannot exceed LKR ${selectedSpecialization.value.maxPhysicalConsultationFee} for this specialization.`
    valid = false
  }

  if (!valid) return

  loading.value = true
  try {
    await doctorService.completeDoctorProfile({
      authId: authResult.value.authId,
      firstName: doctorProfile.firstName,
      lastName: doctorProfile.lastName,
      phone: doctorProfile.phone,
      specialization: doctorProfile.specialization,
      licenseNumber: doctorProfile.licenseNumber,
      yearsOfExperience: doctorProfile.yearsOfExperience,
      consultationFee: {
        video: videoFee,
        physical: physicalFee,
      },
      qualifications: doctorProfile.qualifications
        .split(',')
        .map((q) => q.trim())
        .filter(Boolean),
    })
    currentStep.value = 3
  } catch (e) {
    globalError.value = e?.message || 'Could not save your profile. Please try again.'
  } finally {
    loading.value = false
  }
}

// ── STEP 3: Upload license ──
async function submitStep3() {
  clearErrors()
  if (!authResult.value?.authId) {
    globalError.value = 'Registration session expired. Please start again.'
    return
  }

  const validationError = validateLicenseFile(licenseFile.value)
  if (validationError) {
    fieldErr.license = validationError
    return
  }

  loading.value = true
  try {
    const { uploadUrl } = await doctorService.getLicenseUploadUrl({
      authId: authResult.value.authId,
      fileName: licenseFile.value.name,
    })

    if (!uploadUrl) {
      throw new Error('Could not start file upload. Please try again.')
    }

    await doctorService.uploadFileToPresignedUrl({
      uploadUrl,
      file: licenseFile.value,
    })

    notify.push('Registration submitted successfully.', 'success')
    showPendingApprovalModal.value = true
  } catch (e) {
    globalError.value = e?.message || 'Could not upload your license. Please try again.'
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

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-active .relative,
.modal-leave-active .relative {
  transition: transform 0.2s ease;
}
.modal-enter-from .relative {
  transform: translateY(10px) scale(0.98);
}
.modal-leave-to .relative {
  transform: translateY(6px) scale(0.98);
}
</style>