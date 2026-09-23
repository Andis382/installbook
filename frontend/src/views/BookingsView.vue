<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import {
  PhCalendarCheck,
  PhCheckCircle,
  PhPhoneCall,
  PhWhatsappLogo,
  PhXCircle,
} from '@phosphor-icons/vue'
import AppPage from '@/components/layout/AppPage.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiEmpty from '@/components/ui/UiEmpty.vue'
import UiNotice from '@/components/ui/UiNotice.vue'
import UiSkeleton from '@/components/ui/UiSkeleton.vue'
import UiTabs from '@/components/ui/UiTabs.vue'
import BookingItem from '@/components/bookings/BookingItem.vue'
import ScheduleDialog from '@/components/bookings/ScheduleDialog.vue'
import DeclineDialog from '@/components/bookings/DeclineDialog.vue'
import VisitDialog from '@/components/units/VisitDialog.vue'
import { api, query } from '@/lib/api'
import { unitName } from '@/lib/units'
import type {
  Booking,
  BookingAction,
  BookingList,
  BookingStatus,
  InstallerSettings,
} from '@/lib/types'

const STATUSES: BookingStatus[] = ['NEW', 'SCHEDULED', 'DONE', 'DECLINED']
const ICONS = {
  NEW: PhPhoneCall,
  SCHEDULED: PhCalendarCheck,
  DONE: PhCheckCircle,
  DECLINED: PhXCircle,
}

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const initial = STATUSES.find((s) => s === route.query.tab) ?? 'NEW'
const tab = ref<BookingStatus>(initial)
const list = ref<BookingList | null>(null)
const selected = ref<Booking | null>(null)
const scheduleOpen = ref(false)
const declineOpen = ref(false)
const doneOpen = ref(false)
const shareUrl = ref<string | null>(null)
const price = ref<number | null>(null)

const tabs = computed(() =>
  STATUSES.map((s) => ({
    value: s,
    label: t(`bookings.tabs.${s}`),
    icon: ICONS[s],
    count: list.value ? list.value.counts[s] : null,
  })),
)

async function load() {
  list.value = await api.get<BookingList>(`/bookings${query({ status: tab.value })}`)
}

watch(
  tab,
  (value) => {
    list.value = list.value ? { ...list.value, items: [] } : null
    router.replace({ query: value === 'NEW' ? {} : { tab: value } })
    load()
  },
  { immediate: true },
)

api
  .get<InstallerSettings>('/settings/installer')
  .then((s) => (price.value = s.typicalServicePriceCents))

function open(kind: 'schedule' | 'decline' | 'done', b: Booking) {
  selected.value = b
  shareUrl.value = null
  if (kind === 'schedule') scheduleOpen.value = true
  else if (kind === 'decline') declineOpen.value = true
  else doneOpen.value = true
}

function scheduled(result: BookingAction) {
  shareUrl.value = result.shareUrl
  load()
}
</script>

<template>
  <AppPage :title="$t('bookings.title')" :subtitle="$t('bookings.subtitle')">
    <UiNotice v-if="shareUrl" tone="info" :icon="PhWhatsappLogo">
      {{ $t('bookings.scheduledShare') }}
      <template #actions>
        <UiButton size="sm" :href="shareUrl" target="_blank" :icon="PhWhatsappLogo">{{
          $t('installDone.sendWhatsApp')
        }}</UiButton>
      </template>
    </UiNotice>

    <UiCard padding="none">
      <div class="tabs-wrap">
        <UiTabs v-model="tab" :tabs="tabs" :label="$t('bookings.title')" />
      </div>
      <div class="list">
        <UiSkeleton
          v-if="!list || (!list.items.length && list.counts[tab] > 0)"
          :lines="5"
          height="22px"
        />
        <UiEmpty
          v-else-if="!list.items.length"
          :icon="ICONS[tab]"
          :title="$t(`bookings.empty.${tab}`)"
          :text="$t(`bookings.emptyText.${tab}`)"
          compact
        />
        <template v-else>
          <BookingItem
            v-for="b in list.items"
            :key="b.id"
            :booking="b"
            @schedule="open('schedule', $event)"
            @decline="open('decline', $event)"
            @done="open('done', $event)"
          />
        </template>
      </div>
    </UiCard>

    <ScheduleDialog v-model:open="scheduleOpen" :booking="selected" @saved="scheduled" />
    <DeclineDialog v-model:open="declineOpen" :booking="selected" @saved="load" />
    <VisitDialog
      v-if="selected"
      v-model:open="doneOpen"
      :unit-id="selected.unitId"
      :booking-id="selected.id"
      :unit-label="unitName(selected)"
      :customer-name="selected.customerName"
      :price-cents="price"
      @saved="load"
    />
  </AppPage>
</template>

<style scoped>
.tabs-wrap {
  padding: 8px 12px 0;
  border-bottom: 1px solid var(--border);
  overflow-x: auto;
}
.list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 500px), 1fr));
  align-items: start;
  gap: 12px;
  padding: 16px;
}
.list > :deep(.empty),
.list > :deep(.skeleton) {
  grid-column: 1 / -1;
}
</style>
