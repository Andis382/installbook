<script setup lang="ts">
import { TYPE_COLOR, TYPE_ICON } from '@/lib/units'
import { UNIT_TYPES, type UnitType } from '@/lib/types'

/** Big, glove-friendly type chips with the type's icon; a radio group underneath. */
const model = defineModel<UnitType | null>({ default: null })
defineProps<{ id?: string; label: string; invalid?: boolean }>()

function onKey(e: KeyboardEvent, index: number) {
  const step = e.key === 'ArrowRight' || e.key === 'ArrowDown' ? 1 : e.key === 'ArrowLeft' || e.key === 'ArrowUp' ? -1 : 0
  if (!step) return
  e.preventDefault()
  const next = UNIT_TYPES[(index + step + UNIT_TYPES.length) % UNIT_TYPES.length]!
  model.value = next
  const buttons = (e.currentTarget as HTMLElement).parentElement?.querySelectorAll<HTMLElement>('button')
  buttons?.[(index + step + UNIT_TYPES.length) % UNIT_TYPES.length]?.focus()
}
</script>

<template>
  <div :id="id" class="types" role="radiogroup" :aria-label="label" :aria-invalid="invalid ? 'true' : undefined" tabindex="-1">
    <button
      v-for="(type, i) in UNIT_TYPES"
      :key="type"
      type="button"
      role="radio"
      class="types__opt"
      :class="{ 'is-active': model === type }"
      :aria-checked="model === type"
      :tabindex="model === type || (!model && i === 0) ? 0 : -1"
      :style="{ '--ink': TYPE_COLOR[type].ink, '--tint': TYPE_COLOR[type].soft }"
      @click="model = type"
      @keydown="onKey($event, i)"
    >
      <component :is="TYPE_ICON[type]" :size="22" :weight="model === type ? 'fill' : 'duotone'" aria-hidden="true" />
      <span>{{ $t(`types.${type}`) }}</span>
    </button>
  </div>
</template>

<style scoped>
.types {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(118px, 1fr));
  gap: 8px;
  outline: none;
}
.types__opt {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  min-height: 76px;
  padding: 12px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius);
  background: var(--surface);
  color: var(--text-muted);
  font-size: var(--text-sm);
  font-weight: 650;
  line-height: 1.2;
  text-align: left;
  box-shadow: var(--shadow-xs), var(--highlight);
  transition:
    border-color var(--duration) var(--ease),
    background-color var(--duration) var(--ease),
    box-shadow var(--duration) var(--ease),
    transform var(--duration) var(--ease);
}
.types__opt :deep(svg) {
  color: var(--ink);
}
.types__opt:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--ink) 40%, var(--border-strong));
  box-shadow: var(--shadow-sm), var(--highlight);
}
.types__opt.is-active {
  color: var(--text);
  border-color: var(--ink);
  background: linear-gradient(180deg, var(--surface) -30%, var(--tint) 100%);
  box-shadow:
    0 0 0 3px color-mix(in srgb, var(--ink) 16%, transparent),
    var(--shadow-sm);
}
.types__opt:focus-visible {
  outline: none;
  box-shadow:
    0 0 0 4px var(--focus-ring),
    var(--shadow-sm);
}
.types[aria-invalid='true'] .types__opt {
  border-color: color-mix(in srgb, var(--danger) 45%, var(--border-strong));
}
@media (max-width: 480px) {
  .types {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .types__opt {
    flex-direction: row;
    align-items: center;
    justify-content: flex-start;
    min-height: 52px;
  }
}
</style>
