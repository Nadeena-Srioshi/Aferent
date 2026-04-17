<template>
  <div class="max-w-2xl mx-auto px-5 py-12 pb-20 flex flex-col gap-8">

    <!-- Page intro -->
    <div class="text-center">
      <div class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-[#e0f2f1] text-primary text-[0.75rem] font-bold uppercase tracking-widest mb-4">
        <Sparkles :size="13" :stroke-width="2.5" />
        AI-powered triage
      </div>
      <h1 class="text-4xl font-extrabold text-ink tracking-tight leading-tight mb-3">
        What's bothering you?
      </h1>
      <p class="text-base text-muted leading-relaxed max-w-md mx-auto">
        Describe your symptoms in plain language. Our AI will analyse them and give
        you an initial assessment — instantly.
      </p>
    </div>

    <!-- Main card -->
    <div class="bg-card border-[1.5px] border-border rounded-3xl p-7 flex flex-col gap-6 shadow-sm">
      <SymptomForm
        :loading="loading"
        :has-result="!!result"
        @submit="handleSubmit"
        @clear="handleClear"
      />

      <!-- Divider — only when result is present -->
      <template v-if="result">
        <div class="flex items-center gap-3 text-[0.7rem] font-semibold text-muted uppercase tracking-widest">
          <span class="flex-1 h-px bg-surface" />
          Analysis
          <span class="flex-1 h-px bg-surface" />
        </div>

        <AnalysisResult :result="result" />
      </template>
    </div>

  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Sparkles } from 'lucide-vue-next'
import SymptomForm    from '@/components/sections/SymptomForm.vue'
import AnalysisResult from '@/components/sections/AnalysisResult.vue'
import { analyseSymptoms } from '@/services/symptomService'
import { useNotificationStore } from '@/stores/notificationStore'
import { useAuth } from '@/stores/useAuth'

const notificationStore = useNotificationStore()
const authStore = useAuth()

const loading = ref(false)
const result  = ref(null)

async function handleSubmit(symptoms) {
  loading.value = true
  result.value  = null
  try {
    result.value = await analyseSymptoms(symptoms, authStore.user?.authId ?? null)
    notificationStore.push('Analysis complete. Your results are ready below.', 'success')
  } catch (err) {
    notificationStore.push(err?.message ?? 'Something went wrong. Please try again.', 'error')
  } finally {
    loading.value = false
  }
}

function handleClear() {
  result.value = null
}
</script>