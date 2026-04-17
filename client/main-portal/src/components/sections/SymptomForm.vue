<template>
  <form @submit.prevent="handleSubmit" class="flex flex-col gap-4">

    <!-- Textarea field -->
    <div
      class="border-[1.5px] rounded-2xl bg-card overflow-hidden transition-all duration-200"
      :class="[
        focused  ? 'border-primary shadow-[0_0_0_3px_rgba(0,96,100,0.10)]' : 'border-border',
        hasError ? 'border-danger shadow-[0_0_0_3px_rgba(239,68,68,0.08)]' : '',
      ]"
    >
      <!-- Label + counter -->
      <div class="flex items-center justify-between px-4 pt-3.5">
        <label for="symptoms" class="flex items-center gap-1.5 text-[0.8125rem] font-semibold text-ink">
          <FileText :size="15" :stroke-width="2" />
          Describe your symptoms
        </label>
        <span
          class="text-xs tabular-nums transition-colors"
          :class="charCount > 900 ? 'text-warning font-semibold' : 'text-muted'"
        >
          {{ charCount }}/1000
        </span>
      </div>

      <!-- Textarea -->
      <textarea
        id="symptoms"
        v-model="text"
        rows="6"
        maxlength="1000"
        :disabled="loading"
        placeholder="e.g. I've had a persistent headache for two days, with mild nausea and sensitivity to light…"
        class="block w-full px-4 pt-3 pb-4 bg-transparent border-none outline-none resize-none text-[0.9375rem] leading-relaxed text-ink placeholder:text-muted/60 disabled:opacity-50 disabled:cursor-not-allowed"
        @focus="focused = true"
        @blur="focused = false"
      />

      <!-- Inline error -->
      <p v-if="hasError" class="flex items-center gap-1.5 px-4 pb-3 text-[0.8rem] text-danger">
        <AlertCircle :size="13" :stroke-width="2.5" />
        {{ errorMsg }}
      </p>
    </div>

    <!-- Disclaimer -->
    <p class="flex items-start gap-1.5 text-[0.775rem] text-muted leading-relaxed px-0.5">
      <Info :size="13" :stroke-width="2" class="shrink-0 mt-0.5" />
      This tool is for informational purposes only and does not replace professional medical advice.
    </p>

    <!-- Actions -->
    <div class="flex items-center justify-end gap-3">
      <button
        v-if="hasResult"
        type="button"
        :disabled="loading"
        class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-[0.9rem] font-semibold border-[1.5px] border-border text-muted bg-transparent hover:bg-surface hover:text-ink transition-all disabled:opacity-50 disabled:cursor-not-allowed active:scale-[0.98]"
        @click="$emit('clear')"
      >
        <RotateCcw :size="15" :stroke-width="2" />
        Start over
      </button>

      <button
        type="submit"
        :disabled="loading || !text.trim()"
        class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-[0.9rem] font-semibold bg-primary text-white hover:bg-[#00474b] transition-all disabled:opacity-50 disabled:cursor-not-allowed active:scale-[0.98]"
      >
        <Loader2 v-if="loading" :size="16" :stroke-width="2" class="animate-spin" />
        <Stethoscope v-else :size="16" :stroke-width="2" />
        {{ loading ? 'Analysing…' : 'Analyse symptoms' }}
      </button>
    </div>

  </form>
</template>

<script setup>
import { ref, computed } from 'vue'
import { FileText, AlertCircle, Info, RotateCcw, Loader2, Stethoscope } from 'lucide-vue-next'

const props = defineProps({
  loading:   { type: Boolean, default: false },
  hasResult: { type: Boolean, default: false },
})

const emit = defineEmits(['submit', 'clear'])

const text    = ref('')
const focused = ref(false)
const errorMsg = ref('')
const hasError = computed(() => !!errorMsg.value)
const charCount = computed(() => text.value.length)

function handleSubmit() {
  errorMsg.value = ''
  if (!text.value.trim()) {
    errorMsg.value = 'Please describe your symptoms before submitting.'
    return
  }
  if (text.value.trim().length < 10) {
    errorMsg.value = 'Please provide a bit more detail so we can help you better.'
    return
  }
  emit('submit', text.value.trim())
}
</script>