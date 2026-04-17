<template>
  <main id="main-content" class="min-h-screen bg-[#0f172a] text-white">
    <!-- Top bar -->
    <div class="bg-[#1e293b] border-b border-white/10 px-4 py-3">
      <div class="max-w-6xl mx-auto flex items-center justify-between gap-4">
        <div class="flex items-center gap-3 min-w-0">
          <button
            class="p-2 rounded-lg hover:bg-white/10 transition-colors"
            title="Leave call"
            @click="confirmLeave"
          >
            <ArrowLeft class="w-5 h-5" />
          </button>
          <div class="min-w-0">
            <h1 class="text-sm font-bold truncate">Video Consultation</h1>
            <p class="text-xs text-white/60 truncate">{{ appointmentId }}</p>
          </div>
        </div>

        <div class="flex items-center gap-2">
          <!-- Timer -->
          <div
            v-if="remainingSeconds !== null"
            class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-mono font-semibold"
            :class="remainingSeconds <= 300 ? 'bg-red-500/20 text-red-400' : 'bg-white/10 text-white/80'"
          >
            <Clock3 class="w-3.5 h-3.5" />
            {{ formatTime(remainingSeconds) }}
          </div>

          <!-- Status badge -->
          <span
            class="px-2.5 py-1 rounded-full text-xs font-semibold"
            :class="sessionStatusClass"
          >
            {{ sessionStatusLabel }}
          </span>
        </div>
      </div>
    </div>

    <!-- Error state -->
    <div v-if="fatalError" class="max-w-lg mx-auto mt-20 text-center px-4">
      <div class="bg-red-500/10 border border-red-500/20 rounded-2xl p-8">
        <AlertCircle class="w-12 h-12 text-red-400 mx-auto mb-4" />
        <h2 class="text-lg font-bold mb-2">Unable to join call</h2>
        <p class="text-sm text-white/60 mb-6">{{ fatalError }}</p>
        <button
          class="px-5 py-2.5 rounded-xl bg-white/10 text-sm font-semibold hover:bg-white/20 transition-colors"
          @click="router.push('/appointments')"
        >
          Back to Appointments
        </button>
      </div>
    </div>

    <!-- Loading state -->
    <div v-else-if="joining" class="max-w-lg mx-auto mt-20 text-center px-4">
      <div class="bg-white/5 border border-white/10 rounded-2xl p-8">
        <div class="w-12 h-12 border-4 border-white/20 border-t-primary rounded-full animate-spin mx-auto mb-4"></div>
        <h2 class="text-lg font-bold mb-2">Joining call…</h2>
        <p class="text-sm text-white/60">Setting up your video and audio</p>
      </div>
    </div>

    <!-- Video area -->
    <div v-else class="max-w-6xl mx-auto px-4 py-6">
      <!-- Dev token notice -->
      <div
        v-if="devMode"
        class="mb-4 rounded-xl border border-yellow-500/20 bg-yellow-500/10 px-4 py-3 text-sm text-yellow-300"
      >
        <strong>Dev mode:</strong> Agora credentials are not configured. The API flow works but video will not connect.
        Session was joined successfully.
      </div>

      <!-- Video area — remote is main, local is PiP overlay -->
      <div class="relative mb-6">
        <!-- Remote video (large) -->
        <div class="relative rounded-2xl overflow-hidden bg-[#1e293b] border border-white/10">
          <div class="aspect-video relative">
            <div id="remote-video" class="absolute inset-0"></div>
            <div v-if="!remoteJoined" class="absolute inset-0 flex flex-col items-center justify-center text-white/40 z-[1]">
              <UserRound class="w-16 h-16 mb-3" />
              <p class="text-sm font-medium">Waiting for {{ userRole === 'DOCTOR' ? 'patient' : 'doctor' }}…</p>
            </div>
          </div>
          <div class="absolute bottom-3 left-3 flex items-center gap-2">
            <span class="bg-black/60 backdrop-blur-sm px-2.5 py-1 rounded-lg text-xs font-semibold">
              {{ userRole === 'DOCTOR' ? 'Patient' : 'Doctor' }}
            </span>
            <span v-if="remoteJoined" class="bg-green-500/80 px-2 py-1 rounded-lg text-xs font-semibold">
              Connected
            </span>
          </div>
        </div>

        <!-- Local video (small PiP overlay) -->
        <div class="absolute bottom-4 right-4 w-48 sm:w-56 rounded-xl overflow-hidden bg-[#0f172a] border-2 border-white/20 shadow-lg z-10">
          <div class="aspect-video" id="local-video"></div>
          <div class="absolute bottom-2 left-2">
            <span class="bg-black/60 backdrop-blur-sm px-2 py-0.5 rounded text-[10px] font-semibold">
              You ({{ userRole }})
            </span>
          </div>
        </div>
      </div>

      <!-- Controls -->
      <div class="flex items-center justify-center gap-3">
        <button
          class="w-12 h-12 rounded-full flex items-center justify-center transition-colors"
          :class="audioEnabled ? 'bg-white/10 hover:bg-white/20' : 'bg-red-500 hover:bg-red-600'"
          :title="audioEnabled ? 'Mute microphone' : 'Unmute microphone'"
          @click="toggleAudio"
        >
          <component :is="audioEnabled ? Mic : MicOff" class="w-5 h-5" />
        </button>

        <button
          class="w-12 h-12 rounded-full flex items-center justify-center transition-colors"
          :class="videoEnabled ? 'bg-white/10 hover:bg-white/20' : 'bg-red-500 hover:bg-red-600'"
          :title="videoEnabled ? 'Turn off camera' : 'Turn on camera'"
          @click="toggleVideo"
        >
          <component :is="videoEnabled ? VideoIcon : VideoOff" class="w-5 h-5" />
        </button>

        <button
          class="w-14 h-14 rounded-full bg-red-500 hover:bg-red-600 flex items-center justify-center transition-colors"
          title="End call"
          @click="confirmLeave"
        >
          <PhoneOff class="w-6 h-6" />
        </button>
      </div>

      <!-- Participant info -->
      <div class="mt-6 grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div class="bg-white/5 border border-white/10 rounded-xl p-4 text-center">
          <p class="text-xs text-white/50 mb-1">Session Status</p>
          <p class="text-sm font-semibold">{{ sessionStatus || 'connecting' }}</p>
        </div>
        <div class="bg-white/5 border border-white/10 rounded-xl p-4 text-center">
          <p class="text-xs text-white/50 mb-1">Doctor</p>
          <p class="text-sm font-semibold" :class="doctorJoined ? 'text-green-400' : 'text-white/40'">
            {{ doctorJoined ? 'Connected' : 'Not joined' }}
          </p>
        </div>
        <div class="bg-white/5 border border-white/10 rounded-xl p-4 text-center">
          <p class="text-xs text-white/50 mb-1">Patient</p>
          <p class="text-sm font-semibold" :class="patientJoined ? 'text-green-400' : 'text-white/40'">
            {{ patientJoined ? 'Connected' : 'Not joined' }}
          </p>
        </div>
      </div>
    </div>

    <!-- Leave confirmation dialog -->
    <Teleport to="body">
      <div v-if="showLeaveDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
        <div class="bg-[#1e293b] border border-white/10 rounded-2xl p-6 max-w-sm mx-4 w-full">
          <h3 class="text-lg font-bold mb-2">End video call?</h3>
          <p class="text-sm text-white/60 mb-6">This will end the consultation session for both participants.</p>
          <div class="flex gap-3">
            <button
              class="flex-1 py-2.5 rounded-xl bg-white/10 text-sm font-semibold hover:bg-white/20 transition-colors"
              @click="showLeaveDialog = false"
            >
              Stay
            </button>
            <button
              class="flex-1 py-2.5 rounded-xl bg-red-500 text-sm font-semibold hover:bg-red-600 transition-colors"
              @click="leaveCall"
            >
              End Call
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </main>
</template>

<script setup>
import { computed, nextTick, onMounted, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  AlertCircle,
  ArrowLeft,
  Clock3,
  Mic,
  MicOff,
  PhoneOff,
  UserRound,
  Video as VideoIcon,
  VideoOff,
} from 'lucide-vue-next'
import { useAuth } from '@/stores/useAuth'
import { joinSession, getSessionStatus, endSession } from '@/services/telemedicineService'

const route = useRoute()
const router = useRouter()
const auth = useAuth()

const appointmentId = computed(() => route.params.appointmentId || '')
const userRole = computed(() => auth.user?.role || 'PATIENT')
const userId = computed(() => auth.user?.authId || '')
const userEmail = computed(() => auth.user?.email || '')

const joining = ref(true)
const fatalError = ref('')
const devMode = ref(false)
const showLeaveDialog = ref(false)

const sessionStatus = ref('')
const doctorJoined = ref(false)
const patientJoined = ref(false)
const remainingSeconds = ref(null)
const remoteJoined = ref(false)

const audioEnabled = ref(true)
const videoEnabled = ref(true)
const localTrackCount = ref(0)

let agoraClient = null
let localTracks = []
let statusPollTimer = null

const sessionStatusLabel = computed(() => {
  if (joining.value) return 'Joining…'
  if (sessionStatus.value === 'in_progress') return 'In Progress'
  if (sessionStatus.value === 'ended') return 'Ended'
  return sessionStatus.value || 'Connecting'
})

const sessionStatusClass = computed(() => {
  if (sessionStatus.value === 'in_progress') return 'bg-green-500/20 text-green-400'
  if (sessionStatus.value === 'ended') return 'bg-red-500/20 text-red-400'
  return 'bg-white/10 text-white/60'
})

function formatTime(totalSeconds) {
  if (totalSeconds == null || totalSeconds < 0) return '--:--'
  const m = Math.floor(totalSeconds / 60)
  const s = totalSeconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

function authParams() {
  return {
    token: auth.token,
    userId: userId.value,
    role: userRole.value,
    userEmail: userEmail.value,
  }
}

async function initCall() {
  if (!appointmentId.value) {
    fatalError.value = 'No appointment ID provided.'
    joining.value = false
    return
  }

  try {
    const data = await joinSession({
      ...authParams(),
      appointmentId: appointmentId.value,
    })

    sessionStatus.value = data.status || 'in_progress'
    remoteJoined.value = Boolean(data.otherParticipantJoined)

    if (!data.appId || !data.token || String(data.token).startsWith('dev-')) {
      devMode.value = true
      joining.value = false
      startStatusPolling()
      return
    }

    // Agora connect
    const AgoraRTC = window.AgoraRTC
    if (!AgoraRTC) {
      fatalError.value = 'Agora SDK not loaded. Please refresh the page.'
      joining.value = false
      return
    }

    agoraClient = AgoraRTC.createClient({ mode: 'rtc', codec: 'vp8' })

    async function subscribeRemote(user, mediaType) {
      await agoraClient.subscribe(user, mediaType)
      if (mediaType === 'video' && user.videoTrack) {
        user.videoTrack.play('remote-video')
      }
      if (mediaType === 'audio' && user.audioTrack) {
        user.audioTrack.play()
      }
      remoteJoined.value = true
    }

    agoraClient.on('user-published', subscribeRemote)

    agoraClient.on('user-unpublished', () => {})

    agoraClient.on('user-left', () => {
      remoteJoined.value = false
    })

    await agoraClient.join(data.appId, data.channelName, data.token, data.uid)

    const tracks = []
    let camTrack = null
    try {
      const mic = await AgoraRTC.createMicrophoneAudioTrack()
      tracks.push(mic)
    } catch {
      audioEnabled.value = false
    }

    try {
      camTrack = await AgoraRTC.createCameraVideoTrack()
      tracks.push(camTrack)
    } catch {
      videoEnabled.value = false
    }

    localTracks = tracks
    localTrackCount.value = tracks.length

    if (tracks.length > 0) {
      await agoraClient.publish(tracks)
    }

    joining.value = false

    // Play local video after DOM renders
    await nextTick()
    if (camTrack) {
      camTrack.play('local-video')
    }

    // Subscribe to users who published before we joined
    for (const user of agoraClient.remoteUsers) {
      if (user.hasAudio) await subscribeRemote(user, 'audio')
      if (user.hasVideo) await subscribeRemote(user, 'video')
    }
    startStatusPolling()
  } catch (err) {
    fatalError.value = err?.message || 'Failed to join the video session.'
    joining.value = false
  }
}

async function pollStatus() {
  try {
    const data = await getSessionStatus({
      ...authParams(),
      appointmentId: appointmentId.value,
    })
    sessionStatus.value = data.status || sessionStatus.value
    doctorJoined.value = Boolean(data.doctorJoined)
    patientJoined.value = Boolean(data.patientJoined)
    remainingSeconds.value =
      typeof data.remainingDurationSec === 'number' ? data.remainingDurationSec : null

    if (userRole.value === 'DOCTOR') {
      remoteJoined.value = data.patientJoined
    } else {
      remoteJoined.value = data.doctorJoined
    }

    if (data.status === 'ended') {
      stopStatusPolling()
    }
  } catch {
    // Silently ignore poll failures
  }
}

function startStatusPolling() {
  pollStatus()
  statusPollTimer = setInterval(pollStatus, 5000)
}

function stopStatusPolling() {
  if (statusPollTimer) {
    clearInterval(statusPollTimer)
    statusPollTimer = null
  }
}

async function toggleAudio() {
  const audioTrack = localTracks.find((t) => t.trackMediaType === 'audio')
  if (!audioTrack) return
  audioEnabled.value = !audioEnabled.value
  audioTrack.setEnabled(audioEnabled.value)
}

async function toggleVideo() {
  const videoTrack = localTracks.find((t) => t.trackMediaType === 'video')
  if (!videoTrack) return
  videoEnabled.value = !videoEnabled.value
  videoTrack.setEnabled(videoEnabled.value)
}

function confirmLeave() {
  showLeaveDialog.value = true
}

async function leaveCall() {
  showLeaveDialog.value = false
  stopStatusPolling()

  try {
    await endSession({
      ...authParams(),
      appointmentId: appointmentId.value,
    })
  } catch {
    // Best-effort end
  }

  for (const t of localTracks) {
    try { t.stop(); t.close() } catch {}
  }
  localTracks = []
  localTrackCount.value = 0

  if (agoraClient) {
    try { await agoraClient.leave() } catch {}
    agoraClient = null
  }

  router.push('/appointments')
}

onMounted(() => {
  initCall()
})

onBeforeUnmount(() => {
  stopStatusPolling()
  for (const t of localTracks) {
    try { t.stop(); t.close() } catch {}
  }
  localTracks = []
  if (agoraClient) {
    try { agoraClient.leave() } catch {}
    agoraClient = null
  }
})
</script>
