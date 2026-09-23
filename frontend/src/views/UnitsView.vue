<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { PhBarcode, PhMagnifyingGlass, PhPlus } from '@phosphor-icons/vue'
import AppPage from '@/components/layout/AppPage.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiSearch from '@/components/ui/UiSearch.vue'
import UiSelect from '@/components/ui/UiSelect.vue'
import UiCheckbox from '@/components/ui/UiCheckbox.vue'
import UiEmpty from '@/components/ui/UiEmpty.vue'
import UiSkeleton from '@/components/ui/UiSkeleton.vue'
import TypeIcon from '@/components/units/TypeIcon.vue'
import SerialChip from '@/components/units/SerialChip.vue'
import UnitPills from '@/components/units/UnitPills.vue'
import { api, query } from '@/lib/api'
import { formatDate, formatPhone } from '@/lib/format'
import { town, unitName } from '@/lib/units'
import { UNIT_TYPES, type Page, type UnitRow } from '@/lib/types'

const PAGE_SIZE = 40
const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const str = (v: unknown) => (typeof v === 'string' ? v : '')
const q = ref(str(route.query.q))
const type = ref(str(route.query.type))
const warranty = ref(str(route.query.warranty))
const service = ref(str(route.query.service))
const removed = ref(route.query.status === 'REMOVED')

const items = ref<UnitRow[] | null>(null)
const total = ref(0)
const page = ref(0)
const loadingMore = ref(false)

const typeOptions = computed(() => [
  { value: '', label: t('units.allTypes') },
  ...UNIT_TYPES.map((v) => ({ value: v as string, label: t(`types.${v}`) })),
])
const warrantyOptions = computed(() => [
  { value: '', label: t('units.warrantyAny') },
  { value: 'ACTIVE', label: t('units.warrantyActive') },
  { value: 'EXPIRED', label: t('units.warrantyExpired') },
])
const serviceOptions = computed(() => [
  { value: '', label: t('units.serviceAny') },
  { value: 'OVERDUE', label: t('units.serviceOverdue') },
  { value: 'DUE', label: t('units.serviceDue') },
  { value: 'OK', label: t('units.serviceOk') },
])
const filtered = computed(
  () => !!(q.value || type.value || warranty.value || service.value || removed.value),
)

/** The filters as they appear in the address bar, so back and reload keep them. */
function filters(): Record<string, string> {
  const all: Record<string, string> = {
    q: q.value.trim(),
    type: type.value,
    warranty: warranty.value,
    service: service.value,
    status: removed.value ? 'REMOVED' : '',
  }
  return Object.fromEntries(Object.entries(all).filter(([, v]) => v !== ''))
}

function params(p: number) {
  return { ...filters(), page: p, size: PAGE_SIZE }
}

let request = 0
async function load() {
  const mine = ++request
  const result = await api.get<Page<UnitRow>>(`/units${query(params(0))}`)
  if (mine !== request) return
  items.value = result.items
  total.value = result.total
  page.value = 0
}

async function more() {
  loadingMore.value = true
  try {
    const result = await api.get<Page<UnitRow>>(`/units${query(params(page.value + 1))}`)
    items.value = [...(items.value ?? []), ...result.items]
    page.value += 1
  } finally {
    loadingMore.value = false
  }
}

let timer: ReturnType<typeof setTimeout> | undefined
watch([q, type, warranty, service, removed], () => {
  clearTimeout(timer)
  timer = setTimeout(() => {
    router.replace({ query: filters() })
    load()
  }, 220)
})
onBeforeUnmount(() => clearTimeout(timer))
load()

function clearFilters() {
  q.value = ''
  type.value = ''
  warranty.value = ''
  service.value = ''
  removed.value = false
}

function open(u: UnitRow) {
  router.push({ name: 'unit', params: { id: u.id } })
}
</script>

<template>
  <AppPage :title="$t('units.title')" :subtitle="$t('units.subtitle')">
    <UiCard padding="sm" class="filters">
      <UiSearch
        v-model="q"
        :placeholder="$t('units.search')"
        :label="$t('common.search')"
        class="filters__search"
      />
      <UiSelect v-model="type" :options="typeOptions" :aria-label="$t('units.type')" />
      <UiSelect v-model="warranty" :options="warrantyOptions" :aria-label="$t('units.warranty')" />
      <UiSelect v-model="service" :options="serviceOptions" :aria-label="$t('units.service')" />
      <UiCheckbox v-model="removed" :label="$t('units.showRemoved')" class="filters__removed" />
    </UiCard>

    <p v-if="items" class="count small muted" role="status">
      {{ $t('units.count', { n: total }, total) }}
      <button v-if="filtered" type="button" class="count__clear" @click="clearFilters">
        {{ $t('units.clearFilters') }}
      </button>
    </p>

    <UiCard v-if="!items" padding="md"><UiSkeleton :lines="8" height="22px" /></UiCard>

    <UiCard v-else-if="!items.length" padding="lg">
      <UiEmpty
        v-if="filtered"
        :icon="PhMagnifyingGlass"
        :title="$t('units.empty')"
        :text="$t('units.emptyText')"
      >
        <UiButton variant="secondary" @click="clearFilters">{{
          $t('units.clearFilters')
        }}</UiButton>
      </UiEmpty>
      <UiEmpty
        v-else
        :icon="PhBarcode"
        :title="$t('units.emptyAll')"
        :text="$t('units.emptyAllText')"
      >
        <UiButton variant="accent" :icon="PhPlus" :to="{ name: 'install' }">{{
          $t('today.recordInstall')
        }}</UiButton>
      </UiEmpty>
    </UiCard>

    <template v-else>
      <UiCard padding="none" class="desk">
        <div class="table-wrap">
          <table class="table units">
            <thead>
              <tr>
                <th scope="col">{{ $t('units.colUnit') }}</th>
                <th scope="col">{{ $t('units.colSerial') }}</th>
                <th scope="col">{{ $t('units.colCustomer') }}</th>
                <th scope="col">{{ $t('units.colInstalled') }}</th>
                <th scope="col">{{ $t('units.colService') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in items" :key="u.id" class="units__row" @click="open(u)">
                <td>
                  <div class="units__unit">
                    <TypeIcon :type="u.type" size="sm" />
                    <div class="units__names">
                      <RouterLink
                        :to="{ name: 'unit', params: { id: u.id } }"
                        class="units__link"
                        @click.stop
                        >{{ unitName(u) }}</RouterLink
                      >
                      <span class="xsmall subtle">{{ $t(`types.${u.type}`) }}</span>
                    </div>
                  </div>
                </td>
                <td><SerialChip :serial="u.serialNumber" size="sm" /></td>
                <td>
                  <div class="units__names">
                    <span class="strong">{{ u.customerName }}</span>
                    <span class="xsmall subtle num"
                      >{{ formatPhone(u.customerPhone) }} · {{ town(u.address) }}</span
                    >
                  </div>
                </td>
                <td class="num nowrap">{{ formatDate(u.installedOn) }}</td>
                <td>
                  <UnitPills
                    :warranty-active="u.warrantyActive"
                    :warranty-until="u.warrantyUntil"
                    :service-state="u.serviceState"
                    :days-until-due="u.daysUntilDue"
                    :next-service-due="u.nextServiceDue"
                    :status="u.status"
                    size="sm"
                    class="units__pills"
                  />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </UiCard>

      <ul class="phone">
        <li v-for="u in items" :key="u.id">
          <RouterLink :to="{ name: 'unit', params: { id: u.id } }" class="tile">
            <div class="tile__top">
              <TypeIcon :type="u.type" />
              <div class="tile__names">
                <span class="tile__name">{{ u.customerName }}</span>
                <span class="tile__unit">{{ unitName(u) }} · {{ town(u.address) }}</span>
              </div>
            </div>
            <SerialChip :serial="u.serialNumber" size="sm" />
            <UnitPills
              :warranty-active="u.warrantyActive"
              :warranty-until="u.warrantyUntil"
              :service-state="u.serviceState"
              :days-until-due="u.daysUntilDue"
              :next-service-due="u.nextServiceDue"
              :status="u.status"
              size="sm"
            />
          </RouterLink>
        </li>
      </ul>

      <div v-if="items.length < total" class="more">
        <UiButton variant="secondary" :loading="loadingMore" @click="more">{{
          $t('common.showMore')
        }}</UiButton>
      </div>
    </template>
  </AppPage>
</template>

<style scoped>
.filters :deep(.card__body) {
  display: grid;
  grid-template-columns: minmax(0, 2.2fr) repeat(3, minmax(0, 1fr)) auto;
  align-items: center;
  gap: 10px;
}
.filters__removed {
  white-space: nowrap;
}
.count {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: -6px 2px -8px;
}
.count__clear {
  border: 0;
  background: none;
  padding: 0;
  color: var(--primary);
  font-weight: 650;
  font-size: var(--text-sm);
}
.count__clear:hover {
  text-decoration: underline;
}
.units__row {
  cursor: pointer;
}
.units__unit {
  display: flex;
  align-items: center;
  gap: 10px;
}
.units__names {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.units__link {
  font-weight: 650;
  color: var(--text);
  text-decoration: none;
}
.units__row:hover .units__link {
  color: var(--primary);
  text-decoration: underline;
}
.units__pills {
  flex-direction: column;
  align-items: flex-start;
}
.phone {
  display: none;
  flex-direction: column;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.tile {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  box-shadow: var(--shadow-sm), var(--highlight);
  color: var(--text);
  text-decoration: none;
}
.tile:active {
  transform: scale(0.99);
}
.tile__top {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}
.tile__names {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.tile__name {
  font-weight: 700;
}
.tile__unit {
  font-size: var(--text-sm);
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.more {
  display: flex;
  justify-content: center;
}
@media (max-width: 980px) {
  .filters :deep(.card__body) {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .filters__search {
    grid-column: 1 / -1;
  }
}
@media (max-width: 760px) {
  .desk {
    display: none;
  }
  .phone {
    display: flex;
  }
  .filters :deep(.card__body) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
