<script setup lang="ts">
import { computed } from 'vue'
import { PhBellRinging, PhCalendarCheck, PhCamera } from '@phosphor-icons/vue'
import WarrantyCertificate from '@/components/units/WarrantyCertificate.vue'
import { addMonths } from '@/lib/dates'
import { todayIso } from '@/lib/format'

// A sample card dated from today, so the example never looks stale.
const installedOn = computed(() => addMonths(todayIso(), -11))
const points = [
  { key: 'plate', icon: PhCamera },
  { key: 'reminder', icon: PhBellRinging },
  { key: 'due', icon: PhCalendarCheck },
]
</script>

<template>
  <div class="aside">
    <h2 class="aside__title">{{ $t('aside.title') }}</h2>
    <ul class="aside__points">
      <li v-for="p in points" :key="p.key">
        <span class="aside__icon"
          ><component :is="p.icon" :size="18" weight="bold" aria-hidden="true"
        /></span>
        <span>{{ $t(`aside.points.${p.key}`) }}</span>
      </li>
    </ul>
    <div class="aside__card" aria-hidden="true">
      <WarrantyCertificate
        business="Termo Hoxha"
        customer="Mira Kola"
        type="BOILER"
        brand="Vaillant"
        model="ecoTEC plus"
        serial="21242400100914044N1"
        :installed-on="installedOn"
        :warranty-until="addMonths(installedOn, 24)"
        :warranty-active="true"
        :next-service-due="addMonths(installedOn, 12)"
        card-number="IB-0BWS18B"
        compact
      />
    </div>
  </div>
</template>

<style scoped>
.aside__title {
  color: var(--header-text);
  font-size: clamp(1.9rem, 1.2rem + 1.8vw, 2.7rem);
  font-weight: 700;
  letter-spacing: -0.035em;
  line-height: 1.08;
  max-width: 15ch;
}
.aside__points {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 26px 0 0;
  padding: 0;
  list-style: none;
  color: var(--text-inverse-muted);
  max-width: 46ch;
}
.aside__points li {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.aside__icon {
  display: grid;
  place-items: center;
  flex: none;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  color: var(--accent-300);
  background: color-mix(in srgb, var(--header-text) 8%, transparent);
  border: 1px solid color-mix(in srgb, var(--header-text) 14%, transparent);
}
.aside__card {
  margin-top: 34px;
  max-width: 400px;
  transform: rotate(-2.5deg);
  filter: drop-shadow(0 24px 40px rgb(0 0 0 / 0.4));
}
@media (max-height: 820px) {
  .aside__card {
    display: none;
  }
}
</style>
