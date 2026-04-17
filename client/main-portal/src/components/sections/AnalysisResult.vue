<template>
  <Transition
    enter-active-class="transition-all duration-300 ease-out"
    enter-from-class="opacity-0 translate-y-2"
  >
    <div v-if="result" class="flex flex-col gap-5">

      <!-- Header -->
      <div class="flex items-start justify-between gap-4 flex-wrap">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-[#e0f2f1] text-primary flex items-center justify-center shrink-0">
            <Stethoscope :size="18" :stroke-width="1.75" />
          </div>
          <div>
            <p class="text-[0.725rem] font-bold text-muted uppercase tracking-widest mb-0.5">Analysis result</p>
            <h2 class="text-[1.15rem] font-bold text-ink tracking-tight">{{ formattedCategory }}</h2>
          </div>
        </div>
        <ConfidenceBadge :tier="result.confidence_tier" />
      </div>

      <!-- Alert banners -->
      <div
        v-if="result.escalate_to_human"
        class="flex items-start gap-2.5 px-4 py-3.5 rounded-xl bg-red-50 border border-red-200 text-red-800 text-sm"
      >
        <AlertTriangle :size="16" :stroke-width="2" class="shrink-0 mt-0.5" />
        <div>
          <strong class="font-semibold">Urgent — Please seek medical attention.</strong>
          <span class="opacity-80"> This case has been flagged for immediate human review.</span>
        </div>
      </div>
      <div
        v-else-if="result.verification_required"
        class="flex items-start gap-2.5 px-4 py-3.5 rounded-xl bg-yellow-50 border border-yellow-200 text-yellow-900 text-sm"
      >
        <Info :size="16" :stroke-width="2" class="shrink-0 mt-0.5" />
        <div>
          <strong class="font-semibold">Verification recommended.</strong>
          <span class="opacity-80"> A healthcare professional should review these findings.</span>
        </div>
      </div>

      <!-- Confidence score bar -->
      <div class="flex flex-col gap-1.5">
        <div class="flex justify-between text-xs font-medium text-muted">
          <span>Confidence score</span>
          <span class="font-bold text-ink tabular-nums">{{ scorePercent }}%</span>
        </div>
        <div class="h-1.5 bg-surface rounded-full overflow-hidden">
          <div
            class="h-full rounded-full transition-all duration-700 ease-out"
            :class="{
              'bg-success': result.confidence_tier === 'high',
              'bg-warning': result.confidence_tier === 'medium',
              'bg-danger':  result.confidence_tier === 'low',
            }"
            :style="{ width: scorePercent + '%' }"
          />
        </div>
      </div>

      <!-- Reasoning -->
      <div class="flex flex-col gap-2">
        <p class="flex items-center gap-1.5 text-[0.75rem] font-bold text-muted uppercase tracking-widest">
          <Brain :size="14" :stroke-width="2" />
          Clinical reasoning
        </p>
        <p class="text-[0.9375rem] text-ink/80 leading-relaxed">{{ result.reasoning }}</p>
      </div>

      <!-- Suggestions -->
      <div v-if="result.suggestions?.length" class="flex flex-col gap-2">
        <p class="flex items-center gap-1.5 text-[0.75rem] font-bold text-muted uppercase tracking-widest">
          <ListChecks :size="14" :stroke-width="2" />
          Recommendations
        </p>
        <ol class="flex flex-col gap-2 list-none p-0 m-0">
          <li
            v-for="(s, i) in result.suggestions"
            :key="i"
            class="flex items-baseline gap-3 text-[0.9125rem] text-ink/80 leading-relaxed"
          >
            <span class="shrink-0 w-5 h-5 rounded-full bg-[#e0f2f1] text-primary text-[0.65rem] font-bold flex items-center justify-center">
              {{ i + 1 }}
            </span>
            {{ s }}
          </li>
        </ol>
      </div>

      <!-- Meta footer -->
      <div class="flex items-center gap-4 flex-wrap pt-3 border-t border-surface text-[0.7rem] text-muted/70 tabular-nums">
        <span class="flex items-center gap-1">
          <Hash :size="11" :stroke-width="2" />{{ result.request_id }}
        </span>
        <span v-if="result.patient_id" class="flex items-center gap-1">
          <User :size="11" :stroke-width="2" />{{ result.patient_id }}
        </span>
      </div>

    </div>
  </Transition>
</template>

<script setup>
import { computed } from 'vue'
import { Stethoscope, AlertTriangle, Info, Brain, ListChecks, Hash, User } from 'lucide-vue-next'
import ConfidenceBadge from '@/components/shared/ConfidenceBadge.vue'

const props = defineProps({
  result: { type: Object, default: null },
})

const formattedCategory = computed(() =>
  props.result?.category
    ? props.result.category.charAt(0).toUpperCase() + props.result.category.slice(1)
    : ''
)

const scorePercent = computed(() =>
  props.result ? Math.round(props.result.confidence_score * 100) : 0
)
</script>