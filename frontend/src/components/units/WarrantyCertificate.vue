<script setup lang="ts">
import { computed } from 'vue'
import TypeIcon from './TypeIcon.vue'
import SerialChip from './SerialChip.vue'
import { formatDate, formatMonth } from '@/lib/format'
import { unitName } from '@/lib/units'
import type { UnitType } from '@/lib/types'

/** The warranty card as a customer sees it: printed-certificate paper, the plate facts, a seal. */
const props = withDefaults(
  defineProps<{
    business: string
    customer: string
    type: UnitType
    brand: string
    model: string | null
    serial: string | null
    installedOn: string
    warrantyUntil: string
    warrantyActive: boolean
    nextServiceDue: string | null
    cardNumber?: string | null
    compact?: boolean
  }>(),
  { cardNumber: null, compact: false },
)

const year = computed(() => props.installedOn.slice(0, 4))
</script>

<template>
  <article class="cert" :class="{ 'cert--compact': compact }">
    <div class="cert__guilloche" aria-hidden="true" />
    <header class="cert__head">
      <div>
        <p class="cert__eyebrow">{{ $t('card.certificate') }}</p>
        <p class="cert__business">{{ business }}</p>
      </div>
      <p v-if="cardNumber" class="cert__no mono">{{ cardNumber }}</p>
    </header>

    <div class="cert__unit">
      <TypeIcon :type="type" :size="compact ? 'md' : 'lg'" />
      <div class="cert__names">
        <p class="cert__type">{{ $t(`types.${type}`) }}</p>
        <p class="cert__model">{{ unitName({ brand, model }) }}</p>
      </div>
    </div>

    <div class="cert__row">
      <div class="cert__serial">
        <span class="cert__label">{{ $t('card.serial') }}</span>
        <SerialChip :serial="serial" :size="compact ? 'md' : 'lg'" />
      </div>
      <svg class="cert__seal" viewBox="0 0 80 80" aria-hidden="true">
        <circle cx="40" cy="40" r="36" fill="none" stroke="currentColor" stroke-width="2.2" />
        <circle
          cx="40"
          cy="40"
          r="30.5"
          fill="none"
          stroke="currentColor"
          stroke-width="0.8"
          stroke-dasharray="2 2.4"
        />
        <path
          d="M28 40.5l8 8 16-17"
          fill="none"
          stroke="currentColor"
          stroke-width="4"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
        <text
          x="40"
          y="64"
          text-anchor="middle"
          font-size="8"
          font-weight="700"
          fill="currentColor"
          letter-spacing="1"
        >
          {{ year }}
        </text>
      </svg>
    </div>

    <dl class="cert__facts">
      <div>
        <dt>{{ $t('card.installedOn') }}</dt>
        <dd class="num">{{ formatDate(installedOn) }}</dd>
      </div>
      <div>
        <dt>{{ $t('card.warrantyUntil') }}</dt>
        <dd class="num">
          {{ formatDate(warrantyUntil) }}
          <span class="cert__state" :class="warrantyActive ? 'is-on' : 'is-off'">
            {{ warrantyActive ? $t('card.warrantyActive') : $t('card.warrantyEnded') }}
          </span>
        </dd>
      </div>
      <div v-if="nextServiceDue">
        <dt>{{ $t('card.nextService') }}</dt>
        <dd>{{ formatMonth(nextServiceDue) }}</dd>
      </div>
      <div>
        <dt>{{ $t('card.issuedTo') }}</dt>
        <dd>{{ customer }}</dd>
      </div>
    </dl>
  </article>
</template>

<style scoped>
.cert {
  position: relative;
  overflow: hidden;
  isolation: isolate;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 26px 26px 24px;
  border-radius: var(--radius-lg);
  color: var(--text);
  background: var(--paper);
  border: 1px solid color-mix(in srgb, var(--brand-600) 30%, transparent);
  box-shadow:
    inset 0 0 0 5px var(--paper),
    inset 0 0 0 6px color-mix(in srgb, var(--accent-500) 55%, transparent),
    var(--shadow-lg);
}
.cert--compact {
  gap: 14px;
  padding: 20px;
}
/* Interlaced rings from two corners: the moiré of printed security paper */
.cert__guilloche {
  position: absolute;
  inset: 0;
  z-index: -1;
  background:
    repeating-radial-gradient(circle at 0% 0%, transparent 0 9px, var(--paper-line) 9px 10px),
    repeating-radial-gradient(circle at 100% 0%, transparent 0 9px, var(--paper-line) 9px 10px);
  mask-image: linear-gradient(180deg, #000 0%, rgb(0 0 0 / 0.35) 32%, transparent 58%);
}
.cert__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.cert__eyebrow {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--accent-700);
}
.cert__business {
  margin-top: 2px;
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 700;
  color: var(--brand-900);
}
.cert__no {
  padding: 4px 8px;
  border-radius: 6px;
  background: color-mix(in srgb, var(--paper) 70%, var(--brand-100));
  color: var(--brand-700);
  font-size: var(--text-xs);
  font-weight: 700;
  white-space: nowrap;
}
.cert__unit {
  display: flex;
  align-items: center;
  gap: 14px;
}
.cert__names {
  min-width: 0;
}
.cert__type {
  font-size: var(--text-xs);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-subtle);
}
.cert__model {
  font-family: var(--font-display);
  font-size: clamp(1.15rem, 1rem + 0.8vw, 1.45rem);
  font-weight: 650;
  line-height: 1.2;
  letter-spacing: -0.02em;
}
.cert--compact .cert__model {
  font-size: var(--text-lg);
}
.cert__serial {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}
.cert__label,
.cert__facts dt {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-subtle);
}
.cert__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 20px;
  margin: 0;
  padding-top: 16px;
  border-top: 1px dashed color-mix(in srgb, var(--brand-600) 25%, transparent);
}
.cert__facts dd {
  margin: 2px 0 0;
  font-weight: 600;
}
.cert__state {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 8px;
  border-radius: var(--radius-pill);
  font-size: 11px;
  font-weight: 700;
  vertical-align: 1px;
}
.cert__state.is-on {
  color: var(--success-text);
  background: var(--success-soft);
}
.cert__state.is-off {
  color: var(--text-muted);
  background: var(--surface-sunken);
}
.cert__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.cert__serial {
  min-width: 0;
}
.cert__seal {
  flex: none;
  width: 68px;
  height: 68px;
  color: color-mix(in srgb, var(--brand-600) 55%, transparent);
  transform: rotate(-12deg);
}
.cert--compact .cert__seal {
  width: 54px;
  height: 54px;
}
@media (max-width: 420px) {
  .cert {
    padding: 20px 18px;
  }
  .cert__seal {
    width: 48px;
    height: 48px;
  }
  /* a 19-character serial must fit whole on a phone: it is the point of the card */
  .cert__serial :deep(.plate) {
    min-height: 34px;
    padding: 4px 10px;
    font-size: 0.86rem;
    letter-spacing: 0.03em;
  }
}
</style>
