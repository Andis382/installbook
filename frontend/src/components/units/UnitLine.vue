<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { PhCaretRight } from '@phosphor-icons/vue'
import TypeIcon from './TypeIcon.vue'
import UnitPills from './UnitPills.vue'
import { formatDate } from '@/lib/format'
import { town, unitName } from '@/lib/units'
import type { UnitRow } from '@/lib/types'

/** One unit as a tappable line: what it is, whose it is, and its service state. */
withDefaults(defineProps<{ unit: UnitRow; showCustomer?: boolean }>(), { showCustomer: true })
</script>

<template>
  <RouterLink :to="{ name: 'unit', params: { id: unit.id } }" class="line">
    <TypeIcon :type="unit.type" size="sm" />
    <span class="line__text">
      <span class="line__title">{{ showCustomer ? unit.customerName : unitName(unit) }}</span>
      <span class="line__sub">
        {{ showCustomer ? unitName(unit) : formatDate(unit.installedOn) }} · {{ town(unit.address) }}
      </span>
    </span>
    <UnitPills
      class="line__pills"
      :warranty-active="unit.warrantyActive"
      :warranty-until="unit.warrantyUntil"
      :service-state="unit.serviceState"
      :days-until-due="unit.daysUntilDue"
      :next-service-due="unit.nextServiceDue"
      :status="unit.status"
      only="service"
      size="sm"
    />
    <PhCaretRight class="line__caret" :size="16" weight="bold" aria-hidden="true" />
  </RouterLink>
</template>

<style scoped>
.line {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 56px;
  padding: 8px 10px;
  margin: 0 -10px;
  border-radius: var(--radius-sm);
  color: var(--text);
  text-decoration: none;
  transition: background-color var(--duration) var(--ease);
}
.line:hover {
  color: var(--text);
  background: var(--surface-hover);
}
.line__text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.line__title {
  font-weight: 650;
  font-size: var(--text-sm);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.line__sub {
  font-size: var(--text-xs);
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.line__pills {
  flex: none;
}
.line__caret {
  flex: none;
  color: var(--gray-400);
}
@media (max-width: 480px) {
  .line {
    flex-wrap: wrap;
  }
  .line__pills {
    order: 3;
    width: 100%;
    padding-left: 42px;
  }
  .line__caret {
    display: none;
  }
}
</style>
