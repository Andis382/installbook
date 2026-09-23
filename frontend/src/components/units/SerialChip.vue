<script setup lang="ts">
import { ref } from 'vue'
import { PhCheck, PhCopy } from '@phosphor-icons/vue'

/** A serial number styled like the stamped metal plate it was read from. */
const props = withDefaults(defineProps<{ serial: string | null; size?: 'sm' | 'md' | 'lg'; copyable?: boolean }>(), {
  size: 'md',
  copyable: false,
})

const copied = ref(false)

async function copy() {
  if (!props.serial) return
  try {
    await navigator.clipboard.writeText(props.serial)
  } catch {
    return
  }
  copied.value = true
  setTimeout(() => (copied.value = false), 1600)
}
</script>

<template>
  <span class="plate" :class="[`plate--${size}`, { 'plate--empty': !serial }]">
    <span class="plate__rivet" aria-hidden="true" />
    <span class="plate__text">{{ serial ?? '—' }}</span>
    <button
      v-if="copyable && serial"
      type="button"
      class="plate__copy"
      :aria-label="copied ? $t('unit.serialCopied') : $t('unit.copySerial')"
      :title="$t('unit.copySerial')"
      @click="copy"
    >
      <component :is="copied ? PhCheck : PhCopy" :size="size === 'lg' ? 18 : 15" weight="bold" aria-hidden="true" />
    </button>
    <span v-else class="plate__rivet" aria-hidden="true" />
    <span class="visually-hidden" aria-live="polite">{{ copied ? $t('unit.serialCopied') : '' }}</span>
  </span>
</template>

<style scoped>
.plate {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  min-height: 30px;
  padding: 3px 10px;
  border: 1px solid var(--plate-edge);
  border-radius: 8px;
  color: var(--plate-ink);
  background:
    repeating-linear-gradient(0deg, rgb(255 255 255 / 0.18) 0 1px, transparent 1px 3px),
    linear-gradient(180deg, var(--plate-from), var(--plate-to));
  box-shadow:
    inset 0 1px 0 rgb(255 255 255 / 0.9),
    inset 0 -1px 0 rgb(0 0 0 / 0.05),
    0 1px 2px rgb(16 24 40 / 0.08);
  font-family: var(--font-mono);
  font-size: 0.86rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  font-variant-numeric: tabular-nums slashed-zero;
  white-space: nowrap;
}
.plate__text {
  overflow: hidden;
  text-overflow: ellipsis;
}
.plate--sm {
  min-height: 24px;
  padding: 1px 8px;
  gap: 6px;
  font-size: 0.78rem;
  border-radius: 6px;
}
.plate--lg {
  min-height: 42px;
  padding: 6px 14px;
  gap: 10px;
  font-size: 1.05rem;
  border-radius: 10px;
}
.plate--empty {
  color: var(--text-subtle);
  letter-spacing: 0;
}
.plate__rivet {
  flex: none;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, var(--surface), var(--gray-400));
  box-shadow: 0 0 0 1px rgb(0 0 0 / 0.08);
}
.plate--sm .plate__rivet {
  width: 5px;
  height: 5px;
}
.plate__copy {
  display: grid;
  place-items: center;
  flex: none;
  width: 28px;
  height: 28px;
  margin: -4px -8px -4px 0;
  border: 0;
  border-radius: 6px;
  color: var(--text-muted);
  background: transparent;
  transition: background-color var(--duration) var(--ease);
}
.plate__copy:hover {
  color: var(--text);
  background: rgb(0 0 0 / 0.06);
}
.plate__copy:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px var(--focus-ring);
}
</style>
