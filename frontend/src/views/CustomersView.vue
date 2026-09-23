<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { PhAddressBook, PhMagnifyingGlass, PhWhatsappLogo } from '@phosphor-icons/vue'
import AppPage from '@/components/layout/AppPage.vue'
import UiAvatar from '@/components/ui/UiAvatar.vue'
import UiBadge from '@/components/ui/UiBadge.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiEmpty from '@/components/ui/UiEmpty.vue'
import UiSearch from '@/components/ui/UiSearch.vue'
import UiSkeleton from '@/components/ui/UiSkeleton.vue'
import { api, query } from '@/lib/api'
import { formatDate, formatPhone } from '@/lib/format'
import type { CustomerRow } from '@/lib/types'

const router = useRouter()
const q = ref('')
const rows = ref<CustomerRow[] | null>(null)

let request = 0
async function load() {
  const mine = ++request
  const result = await api.get<CustomerRow[]>(`/customers${query({ q: q.value.trim() || undefined })}`)
  if (mine === request) rows.value = result
}

let timer: ReturnType<typeof setTimeout> | undefined
watch(q, () => {
  clearTimeout(timer)
  timer = setTimeout(load, 220)
})
onBeforeUnmount(() => clearTimeout(timer))
load()
</script>

<template>
  <AppPage :title="$t('customers.title')" :subtitle="$t('customers.subtitle')">
    <UiCard padding="sm">
      <UiSearch v-model="q" :placeholder="$t('customers.search')" />
    </UiCard>
    <p v-if="rows" class="small muted count" role="status">{{ $t('customers.count', { n: rows.length }, rows.length) }}</p>

    <UiCard v-if="!rows"><UiSkeleton :lines="8" height="22px" /></UiCard>
    <UiCard v-else-if="!rows.length" padding="lg">
      <UiEmpty v-if="q" :icon="PhMagnifyingGlass" :title="$t('customers.empty')" :text="$t('customers.emptyText')" />
      <UiEmpty v-else :icon="PhAddressBook" :title="$t('customers.emptyAll')" :text="$t('customers.emptyAllText')" />
    </UiCard>
    <UiCard v-else padding="none">
      <div class="table-wrap desk">
        <table class="table">
          <thead>
            <tr>
              <th scope="col">{{ $t('customers.colName') }}</th>
              <th scope="col">{{ $t('customers.colPhone') }}</th>
              <th scope="col" class="num">{{ $t('customers.colUnits') }}</th>
              <th scope="col">{{ $t('customers.colWhatsApp') }}</th>
              <th scope="col">{{ $t('customers.colLastInstall') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in rows" :key="c.id" class="clickable" @click="router.push({ name: 'customer', params: { id: c.id } })">
              <td>
                <div class="person">
                  <UiAvatar :name="c.name" :size="32" />
                  <RouterLink :to="{ name: 'customer', params: { id: c.id } }" class="person__name" @click.stop>{{ c.name }}</RouterLink>
                  <UiBadge v-if="c.locale === 'en'" size="sm">EN</UiBadge>
                </div>
              </td>
              <td class="num nowrap">{{ formatPhone(c.phone) }}</td>
              <td class="num">{{ c.activeUnits }}</td>
              <td>
                <UiBadge size="sm" :tone="c.whatsappOptIn ? 'success' : 'neutral'" :icon="PhWhatsappLogo">
                  {{ c.whatsappOptIn ? $t('customers.optedIn') : $t('customers.notOptedIn') }}
                </UiBadge>
              </td>
              <td class="num nowrap">{{ formatDate(c.lastInstalledOn) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <ul class="phone">
        <li v-for="c in rows" :key="c.id">
          <RouterLink :to="{ name: 'customer', params: { id: c.id } }" class="phone__row">
            <UiAvatar :name="c.name" :size="38" />
            <span class="phone__text">
              <span class="strong">{{ c.name }}</span>
              <span class="small muted num">{{ formatPhone(c.phone) }} · {{ $t('units.count', { n: c.activeUnits }, c.activeUnits) }}</span>
            </span>
            <UiBadge size="sm" :tone="c.whatsappOptIn ? 'success' : 'neutral'" :icon="PhWhatsappLogo">
              {{ c.whatsappOptIn ? $t('customers.optedIn') : $t('customers.notOptedIn') }}
            </UiBadge>
          </RouterLink>
        </li>
      </ul>
    </UiCard>
  </AppPage>
</template>

<style scoped>
.count {
  margin: -6px 2px -8px;
}
.clickable {
  cursor: pointer;
}
.person {
  display: flex;
  align-items: center;
  gap: 10px;
}
.person__name {
  font-weight: 650;
  color: var(--text);
  text-decoration: none;
}
.clickable:hover .person__name {
  color: var(--primary);
  text-decoration: underline;
}
.phone {
  display: none;
  margin: 0;
  padding: 0;
  list-style: none;
}
.phone__row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 64px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border);
  color: var(--text);
  text-decoration: none;
}
.phone li:last-child .phone__row {
  border-bottom: 0;
}
.phone__text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
@media (max-width: 760px) {
  .desk {
    display: none;
  }
  .phone {
    display: block;
  }
}
</style>
