<script setup lang="ts">
import { useId } from 'vue'

withDefaults(defineProps<{ size?: number; withName?: boolean; inverse?: boolean }>(), {
  size: 34,
  withName: true,
  inverse: true,
})

// Gradient ids must be unique per instance: a gradient defined inside a hidden copy of the
// logo (the sign-in aside on phones) would otherwise paint every other copy empty.
const id = useId()
</script>

<template>
  <span class="brand" :class="{ 'brand--inverse': inverse }">
    <!-- A riveted data plate with a flame-drop: the thing every installed unit carries -->
    <svg :width="size" :height="size" viewBox="0 0 40 40" aria-hidden="true" class="brand__mark">
      <defs>
        <linearGradient :id="`${id}-plate`" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="#3597c9" />
          <stop offset="1" stop-color="#155a7e" />
        </linearGradient>
        <linearGradient :id="`${id}-flame`" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="#f6bb78" />
          <stop offset="1" stop-color="#d0711c" />
        </linearGradient>
      </defs>
      <rect x="2" y="6" width="36" height="28" rx="7" :fill="`url(#${id}-plate)`" />
      <rect
        x="2.6"
        y="6.6"
        width="34.8"
        height="26.8"
        rx="6.4"
        fill="none"
        stroke="#fff"
        stroke-opacity=".32"
      />
      <circle cx="6.9" cy="10.9" r="1.25" fill="#fff" fill-opacity=".62" />
      <circle cx="33.1" cy="10.9" r="1.25" fill="#fff" fill-opacity=".62" />
      <circle cx="6.9" cy="29.1" r="1.25" fill="#fff" fill-opacity=".62" />
      <circle cx="33.1" cy="29.1" r="1.25" fill="#fff" fill-opacity=".62" />
      <path
        d="M14 13.2c2.9 3.2 4.7 5.8 4.7 8.2a4.7 4.7 0 0 1-9.4 0c0-2.4 1.8-5 4.7-8.2z"
        :fill="`url(#${id}-flame)`"
      />
      <path
        d="M14 19.4c1.1 1.3 1.8 2.3 1.8 3.2a1.8 1.8 0 0 1-3.6 0c0-.9.7-1.9 1.8-3.2z"
        fill="#fff"
        fill-opacity=".88"
      />
      <path
        d="M22.2 15.6h9.6M22.2 20h9.6M22.2 24.4h6"
        stroke="#fff"
        stroke-width="2.3"
        stroke-linecap="round"
      />
    </svg>
    <span v-if="withName" class="brand__name">Install<b>Book</b></span>
  </span>
</template>

<style scoped>
.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--text);
  text-decoration: none;
}
.brand--inverse {
  color: var(--header-text);
}
.brand__mark {
  flex: none;
  filter: drop-shadow(0 4px 10px rgb(0 0 0 / 0.22));
}
.brand__name {
  font-family: var(--font-display);
  font-size: 1.2rem;
  font-weight: 500;
  letter-spacing: -0.02em;
}
.brand__name b {
  font-weight: 700;
}
</style>
