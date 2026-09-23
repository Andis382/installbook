<script setup lang="ts">
import { computed } from 'vue'
import type { UnitType } from '@/lib/types'
import { TYPE_COLOR, TYPE_ICON } from '@/lib/units'

const props = withDefaults(defineProps<{ type: UnitType; size?: 'sm' | 'md' | 'lg' }>(), {
  size: 'md',
})

const px = computed(() => ({ sm: 16, md: 20, lg: 28 })[props.size])
const style = computed(() => ({
  '--ink': TYPE_COLOR[props.type].ink,
  '--tint': TYPE_COLOR[props.type].soft,
}))
</script>

<template>
  <span class="type" :class="`type--${size}`" :style="style" :title="$t(`types.${type}`)">
    <component :is="TYPE_ICON[type]" :size="px" weight="duotone" aria-hidden="true" />
  </span>
</template>

<style scoped>
.type {
  display: inline-grid;
  place-items: center;
  flex: none;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  color: var(--ink);
  background: linear-gradient(180deg, var(--surface) -40%, var(--tint) 100%);
  border: 1px solid color-mix(in srgb, var(--ink) 18%, transparent);
  box-shadow:
    var(--highlight),
    0 1px 2px rgb(16 24 40 / 0.06);
}
.type--sm {
  width: 30px;
  height: 30px;
  border-radius: 9px;
}
.type--lg {
  width: 56px;
  height: 56px;
  border-radius: 16px;
}
</style>
