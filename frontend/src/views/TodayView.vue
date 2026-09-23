<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  PhCalendarCheck,
  PhChartBar,
  PhCurrencyEur,
  PhPhoneCall,
  PhPlus,
  PhTrendUp,
  PhWrench,
} from '@phosphor-icons/vue'
import AppPage from '@/components/layout/AppPage.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiStat from '@/components/ui/UiStat.vue'
import UiEmpty from '@/components/ui/UiEmpty.vue'
import UiSkeleton from '@/components/ui/UiSkeleton.vue'
import UiBarChart from '@/components/ui/UiBarChart.vue'
import BookingItem from '@/components/bookings/BookingItem.vue'
import ScheduleDialog from '@/components/bookings/ScheduleDialog.vue'
import DeclineDialog from '@/components/bookings/DeclineDialog.vue'
import UnitLine from '@/components/units/UnitLine.vue'
import { api } from '@/lib/api'
import {
  formatDate,
  formatMoney,
  formatMonthShort,
  formatNumber,
  formatPercent,
} from '@/lib/format'
import { monthStart } from '@/lib/dates'
import type { Booking, Dashboard } from '@/lib/types'
import { useAuth } from '@/stores/auth'

const { t } = useI18n()
const auth = useAuth()
const data = ref<Dashboard | null>(null)
const scheduling = ref<Booking | null>(null)
const declining = ref<Booking | null>(null)
const scheduleOpen = ref(false)
const declineOpen = ref(false)

async function load() {
  data.value = await api.get<Dashboard>('/dashboard')
}

onMounted(load)

const subtitle = computed(() =>
  [formatDate(data.value?.today ?? new Date(), 'long'), auth.organization?.name]
    .filter(Boolean)
    .join(' · '),
)

const chartRows = computed(() =>
  (data.value?.installsByMonth ?? []).map((m, i, all) => ({
    label: formatMonthShort(monthStart(m.month)),
    values: { installs: m.count },
    highlight: i === all.length - 1,
  })),
)

function schedule(b: Booking) {
  scheduling.value = b
  scheduleOpen.value = true
}

function decline(b: Booking) {
  declining.value = b
  declineOpen.value = true
}
</script>

<template>
  <AppPage :title="$t('today.title')" :subtitle="subtitle">
    <template #actions>
      <UiButton
        class="today__cta"
        variant="accent"
        size="lg"
        :icon="PhPlus"
        :to="{ name: 'install' }"
        >{{ $t('today.recordInstall') }}</UiButton
      >
    </template>

    <div v-if="!data" class="stats">
      <UiSkeleton v-for="n in 4" :key="n" card :lines="2" height="22px" />
    </div>
    <div v-else class="stats">
      <UiStat
        :label="$t('today.dueThisMonth')"
        :value="formatNumber(data.dueThisMonth)"
        :icon="PhCalendarCheck"
        tone="warning"
        :to="{ name: 'due' }"
        :hint="
          data.overdue
            ? $t('today.dueHint', { n: data.overdue }, data.overdue)
            : $t('today.dueHintNone')
        "
      />
      <UiStat
        :label="$t('today.revenue')"
        :value="formatMoney(data.revenueThisMonthCents + data.revenueOverdueCents)"
        :icon="PhCurrencyEur"
        tone="success"
        :to="{ name: 'due' }"
        :hint="
          $t('today.revenueHint', {
            count: data.dueThisMonth + data.overdue,
            price: formatMoney(data.typicalPriceCents),
          })
        "
      />
      <UiStat
        :label="$t('today.conversion')"
        :value="data.conversion.sent ? formatPercent(data.conversion.rate) : '—'"
        :icon="PhTrendUp"
        tone="primary"
        :hint="
          data.conversion.sent
            ? $t('today.conversionHint', {
                booked: data.conversion.booked,
                sent: data.conversion.sent,
              })
            : $t('today.conversionNone')
        "
      />
      <UiStat
        :label="$t('today.installs')"
        :value="formatNumber(data.installsThisMonth)"
        :icon="PhWrench"
        tone="info"
        :to="{ name: 'units' }"
        :hint="$t('today.installsHint', { n: data.installsLastMonth })"
      />
    </div>

    <div class="layout">
      <UiCard
        :title="$t('today.callTitle')"
        :subtitle="$t('today.callSubtitle')"
        :icon="PhPhoneCall"
        class="layout__main"
      >
        <template v-if="data?.newBookings.length" #actions>
          <UiButton variant="ghost" size="sm" :to="{ name: 'bookings' }">{{
            $t('common.viewAll')
          }}</UiButton>
        </template>
        <UiSkeleton v-if="!data" :lines="4" height="20px" />
        <UiEmpty
          v-else-if="!data.newBookings.length"
          :icon="PhPhoneCall"
          :title="$t('today.callEmpty')"
          :text="$t('today.callEmptyText')"
          compact
        />
        <div v-else class="bookings">
          <BookingItem
            v-for="b in data.newBookings"
            :key="b.id"
            :booking="b"
            @schedule="schedule"
            @decline="decline"
          />
        </div>
      </UiCard>

      <div class="layout__side">
        <UiCard :title="$t('today.dueNextTitle')" :icon="PhCalendarCheck">
          <template #actions>
            <UiButton variant="ghost" size="sm" :to="{ name: 'due' }">{{
              $t('today.openDue')
            }}</UiButton>
          </template>
          <UiSkeleton v-if="!data" :lines="4" height="18px" />
          <p v-else-if="!data.dueNext.length" class="muted small">{{ $t('today.dueNextEmpty') }}</p>
          <div v-else class="lines">
            <UnitLine v-for="u in data.dueNext" :key="u.id" :unit="u" />
          </div>
          <template v-if="data" #footer>
            <p class="small muted">
              {{ $t('today.upcoming', { n: data.remindersThisWeek }, data.remindersThisWeek) }}
              {{ $t('today.inService', { n: data.activeUnits }, data.activeUnits) }}.
            </p>
          </template>
        </UiCard>

        <UiCard
          :title="$t('today.installsTitle')"
          :subtitle="$t('today.installsSubtitle')"
          :icon="PhChartBar"
        >
          <UiSkeleton v-if="!data" :lines="3" height="30px" />
          <UiBarChart
            v-else
            :title="$t('today.installsTitle')"
            :series="[
              { key: 'installs', label: t('today.installsSeries'), color: 'var(--brand-500)' },
            ]"
            :rows="chartRows"
            :height="150"
          />
        </UiCard>
      </div>
    </div>

    <ScheduleDialog v-model:open="scheduleOpen" :booking="scheduling" @saved="load" />
    <DeclineDialog v-model:open="declineOpen" :booking="declining" @saved="load" />
  </AppPage>
</template>

<style scoped>
/* On desktop the copper button in the band is always there; phones get it here, big. */
@media (min-width: 900px) {
  .today__cta {
    display: none;
  }
}
.stats {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.layout {
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr);
  align-items: start;
}
.layout__side {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.bookings {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.lines {
  display: flex;
  flex-direction: column;
}
@media (max-width: 1080px) {
  .stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
@media (max-width: 420px) {
  .stats {
    gap: 10px;
  }
}
</style>
