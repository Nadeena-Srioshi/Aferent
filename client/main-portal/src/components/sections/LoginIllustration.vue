<template>
<div class="flex justify-center items-center flex-1 relative z-10">
  <svg width="400" height="510" viewBox="0 0 220 230" fill="none"
       xmlns="http://www.w3.org/2000/svg" style="overflow:visible;">

    <g class="phone-group">
      <rect x="67" y="22" width="86" height="150" rx="14"
            fill="rgba(255,255,255,0.06)" stroke="rgba(255,255,255,0.15)" stroke-width="1"/>
      <rect x="73" y="32" width="74" height="130" rx="8" fill="rgba(255,255,255,0.04)"/>
      <rect x="94" y="25" width="32" height="5" rx="2.5" fill="rgba(255,255,255,0.12)"/>

      <g class="card-calendar">
        <rect x="79" y="40" width="62" height="40" rx="6"
              fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.12)" stroke-width="0.5"/>
        <rect x="79" y="40" width="62" height="12" rx="6" fill="rgba(0,117,154,0.5)"/>
        <rect x="79" y="46" width="62" height="6"         fill="rgba(0,117,154,0.5)"/>
        <text x="110" y="49" text-anchor="middle" font-size="6"
              fill="rgba(255,255,255,0.9)" font-family="system-ui">March 2025</text>
        <circle cx="88"  cy="61" r="2"   fill="rgba(255,255,255,0.2)"/>
        <circle cx="98"  cy="61" r="2"   fill="rgba(255,255,255,0.2)"/>
        <circle cx="108" cy="61" r="6"   fill="rgba(77,208,213,0.15)" class="pulse-dot"/>
        <circle cx="108" cy="61" r="2.5" fill="#4DD0D5"/>
        <circle cx="118" cy="61" r="2"   fill="rgba(255,255,255,0.2)"/>
        <circle cx="128" cy="61" r="2"   fill="rgba(255,255,255,0.2)"/>
        <circle cx="88"  cy="69" r="2"   fill="rgba(255,255,255,0.2)"/>
        <circle cx="98"  cy="69" r="2"   fill="rgba(255,255,255,0.2)"/>
        <circle cx="108" cy="69" r="2"   fill="rgba(255,255,255,0.2)"/>
        <circle cx="118" cy="69" r="2"   fill="rgba(255,255,255,0.2)"/>
        <circle cx="128" cy="69" r="2.5" fill="rgba(77,208,213,0.5)"/>
      </g>

      <g class="card-appt">
        <rect x="79" y="88" width="62" height="32" rx="5"
              fill="rgba(255,255,255,0.08)" stroke="rgba(255,255,255,0.1)" stroke-width="0.5"/>
        <rect x="79" y="88" width="3" height="32" rx="1.5" fill="#4DD0D5"/>
        <text x="87" y="98"  font-size="5.5" fill="rgba(255,255,255,0.5)"  font-family="system-ui">Upcoming</text>
        <text x="87" y="107" font-size="6.5" fill="rgba(255,255,255,0.9)"  font-family="system-ui" font-weight="500">Dr. Sarah Chen</text>
        <text x="87" y="115" font-size="5"   fill="rgba(255,255,255,0.45)" font-family="system-ui">Today · 2:30 PM · Telemedicine</text>
      </g>

      <g class="card-cta">
        <rect x="79" y="128" width="62" height="24" rx="5"
              fill="rgba(0,117,154,0.4)" stroke="rgba(0,117,154,0.6)" stroke-width="0.5"/>
        <text x="110" y="143" text-anchor="middle" font-size="6"
              fill="rgba(255,255,255,0.85)" font-family="system-ui">Join Video Call</text>
        <rect x="86" y="135" width="8" height="6" rx="1.5" fill="rgba(255,255,255,0.3)"/>
        <path d="M94 136.5 L98 138.5 L94 140.5 Z" fill="rgba(255,255,255,0.6)"/>
      </g>

      <circle cx="98"  cy="162" r="2"   fill="rgba(255,255,255,0.15)"/>
      <circle cx="110" cy="162" r="2.5" fill="#4DD0D5"/>
      <circle cx="122" cy="162" r="2"   fill="rgba(255,255,255,0.15)"/>
    </g>

    <g class="notif-top">
      <rect x="124" y="10" width="78" height="30" rx="9"
            fill="rgba(255,255,255,0.11)" stroke="rgba(255,255,255,0.2)" stroke-width="0.5"/>
      <circle cx="136" cy="25" r="8" fill="rgba(0,117,154,0.65)"/>
      <path class="check-path" d="M133 25 L135.5 27.5 L140 22"
            stroke="#4DD0D5" stroke-width="1.4" stroke-linecap="round"
            stroke-linejoin="round" fill="none"/>
      <text x="148" y="21" font-size="5.5" fill="rgba(255,255,255,0.75)"
            font-family="system-ui" font-weight="500">Confirmed!</text>
      <text x="148" y="30" font-size="5" fill="rgba(255,255,255,0.45)"
            font-family="system-ui">Appointment booked</text>
    </g>

    <g class="pill-bottom">
      <rect x="10" y="155" width="64" height="26" rx="9"
            fill="rgba(255,255,255,0.09)" stroke="rgba(255,255,255,0.16)" stroke-width="0.5"/>
      <circle cx="22" cy="168" r="7" fill="rgba(77,208,213,0.2)"/>
      <line x1="22" y1="164" x2="22" y2="172" stroke="#4DD0D5" stroke-width="1.4" stroke-linecap="round"/>
      <line x1="18" y1="168" x2="26" y2="168" stroke="#4DD0D5" stroke-width="1.4" stroke-linecap="round"/>
      <text x="33" y="165" font-size="5"   fill="rgba(255,255,255,0.5)"  font-family="system-ui">Prescription</text>
      <text x="33" y="174" font-size="5.5" fill="rgba(255,255,255,0.85)" font-family="system-ui">Ready to collect</text>
    </g>

  </svg>
</div>
</template>

<script setup>
// Animation styles can be added here
</script>

<style scoped>
@keyframes floatBob {
  0%, 100% { transform: translateY(0); }
  50%       { transform: translateY(-6px); }
}
@keyframes floatUp {
  from { transform: translateY(10px); opacity: 0; }
  to   { transform: translateY(0);    opacity: 1; }
}
@keyframes shimmer {
  0%, 100% { opacity: 0.4; }
  50%       { opacity: 1; }
}
@keyframes notifSlide {
  0%, 100% { opacity: 0; transform: translateX(8px); }
  15%, 75% { opacity: 1; transform: translateX(0); }
}
@keyframes fadeInOut {
  0%, 100% { opacity: 0; transform: translateY(4px); }
  20%, 80% { opacity: 1; transform: translateY(0); }
}
@keyframes checkDraw {
  from { stroke-dashoffset: 20; }
  to   { stroke-dashoffset: 0; }
}

.phone-group    { animation: floatBob 4s ease-in-out infinite; transform-origin: center; }
.card-calendar  { animation: floatUp 0.6s ease-out 0.2s both; }
.card-appt      { animation: floatUp 0.6s ease-out 0.5s both; }
.card-cta       { animation: floatUp 0.6s ease-out 0.8s both; }
.notif-top      { animation: notifSlide 5s ease-in-out 1s infinite; }
.pill-bottom    { animation: fadeInOut 5s ease-in-out 3s infinite; }
.pulse-dot      { animation: shimmer 2s ease-in-out infinite; }
.check-path     { stroke-dasharray: 20; animation: checkDraw 0.4s ease-out 1.5s both; }
</style>
