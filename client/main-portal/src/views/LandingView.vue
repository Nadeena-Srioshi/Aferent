<template>
  <main id="main-content">
    <HeroSection />
    <SearchSection />
    <HowItWorksSection />
    <SpecialtiesSection />
    <TrustBannerSection />
    <AiInsightsSection />
    <CtaSection />
  </main>
</template>

<script setup>
import HeroSection        from '@/components/sections/HeroSection.vue'
import SearchSection      from '@/components/sections/SearchSection.vue'
import HowItWorksSection  from '@/components/sections/HowItWorksSection.vue'
import SpecialtiesSection from '@/components/sections/SpecialtiesSection.vue'
import TrustBannerSection from '@/components/sections/TrustBannerSection.vue'
import AiInsightsSection  from '@/components/sections/AiInsightsSection.vue'
import CtaSection         from '@/components/sections/CtaSection.vue'
</script>