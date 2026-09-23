<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  PhBellRinging,
  PhCaretLeft,
  PhCaretRight,
  PhCheckCircle,
  PhEnvelopeSimple,
  PhPaperPlaneTilt,
  PhPhone,
  PhWarningCircle,
  PhWhatsappLogo,
} from '@phosphor-icons/vue'
import AppPage from '@/components/layout/AppPage.vue'
import UiBadge from '@/components/ui/UiBadge.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiIconButton from '@/components/ui/UiIconButton.vue'
import UiMenu, { type MenuItem } from '@/components/ui/UiMenu.vue'
import UiSkeleton from '@/components/ui/UiSkeleton.vue'
import TypeIcon from '@/components/units/TypeIcon.vue'
import { api, ApiError, query } from '@/lib/api'
import { formatDay, formatMoney, formatMonth } from '@/lib/format'
import { monthStart, shiftMonth } from '@/lib/dates'
import { LONG_OVERDUE_DAYS, REMINDER_TONE, town, unitName } from '@/lib/units'
import { telLink } from '@/lib/whatsapp'
import type { DueGroup, DueRow, DueView, RunResult } from '@/lib/types'
import { useAuth } from '@/stores/auth'
import { useToasts } from '@/stores/toasts'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const auth = useAuth()
const toasts = useToasts()

const view = ref<DueView | null>(null)
const running = ref(false)
const sendingId = ref<number | null>(null)
const month = computed(() => (typeof route.query.month === 'string' ? route.query.month : ''))
const isCurrent = computed(() => !!view.value && view.value.month === view.value.currentMonth)

async function load() {
  view.value = await api.get<DueView>(`/due${query({ month: month.value || undefined })}`)
}

watch(month, load, { immediate: true })

function go(delta: number) {
  if (!view.value) return
  const next = shiftMonth(view.value.month, delta)
  router.replace({ query: next === view.value.currentMonth ? {} : { month: next } })
}

function groupTitle(g: DueGroup) {
  if (g.group === 'OVERDUE') return t('due.groups.OVERDUE')
  const name = formatMonth(monthStart(g.month!))
  return isCurrent.value ? `${t(`due.groups.${g.group}`)} · ${name}` : name
}

function stateText(r: DueRow) {
  const date =
    r.reminderState === 'BOOKING_SCHEDULED' && r.bookingScheduledAt
      ? formatDay(r.bookingScheduledAt)
      : r.reminderState === 'SENT' && r.reminderSentAt
        ? formatDay(r.reminderSentAt)
        : formatDay(r.reminderGoesOutOn)
  return t(`reminder.${r.reminderState}`, { date })
}

function whenText(r: DueRow) {
  const days = r.unit.daysUntilDue
  if (-days > LONG_OVERDUE_DAYS)
    return t('due.overdueSince', { month: formatMonth(r.unit.nextServiceDue) })
  if (days < 0) return t('due.overdueBy', { n: -days }, -days)
  return t('due.dueOn', { date: formatDay(r.unit.nextServiceDue) })
}

function canRemind(r: DueRow) {
  return r.whatsappOptIn && ['PLANNED', 'WAITING', 'FAILED'].includes(r.reminderState)
}

async function remind(r: DueRow) {
  sendingId.value = r.unit.id
  try {
    await api.post(`/units/${r.unit.id}/remind`)
    toasts.success(t('due.remindSent', { name: r.unit.customerName }))
    await load()
  } catch (e) {
    toasts.error(e instanceof ApiError && e.status ? e.message : t('errors.generic'))
  } finally {
    sendingId.value = null
  }
}

async function runNow() {
  running.value = true
  try {
    const result = await api.post<RunResult>('/reminders/run')
    if (result.sent + result.noConsent + result.failed === 0) toasts.info(t('due.runNothing'))
    else toasts.success(t('due.runResult', { sent: result.sent, noConsent: result.noConsent }))
    await load()
  } finally {
    running.value = false
  }
}

async function digest() {
  try {
    await api.post('/reminders/digest')
    toasts.success(t('due.digestSent'))
  } catch (e) {
    toasts.error(e instanceof ApiError && e.status ? e.message : t('errors.generic'))
  }
}

const ownerMenu = computed<MenuItem[]>(() => [
  { label: t('due.digest'), icon: PhEnvelopeSimple, action: digest },
])
</script>

<template>
  <AppPage :title="$t('due.title')" :subtitle="$t('due.subtitle')">
    <template v-if="auth.hasRole('OWNER')" #actions>
      <UiButton :icon="PhPaperPlaneTilt" :loading="running" @click="runNow">{{
        $t('due.runNow')
      }}</UiButton>
      <UiMenu class="hero-menu" :items="ownerMenu" :label="$t('common.more')" />
    </template>

    <div class="monthbar">
      <UiIconButton
        :icon="PhCaretLeft"
        :label="$t('due.prevMonth')"
        variant="secondary"
        :disabled="!view || isCurrent"
        @click="go(-1)"
      />
      <p class="monthbar__label">{{ view ? formatMonth(monthStart(view.month)) : '' }}</p>
      <UiIconButton
        :icon="PhCaretRight"
        :label="$t('due.nextMonth')"
        variant="secondary"
        :disabled="!view"
        @click="go(1)"
      />
      <UiButton
        v-if="view && !isCurrent"
        variant="ghost"
        size="sm"
        class="monthbar__back"
        @click="router.replace({ query: {} })"
      >
        {{ $t('due.backToThisMonth') }}
      </UiButton>
    </div>

    <template v-if="!view">
      <UiSkeleton v-for="n in 3" :key="n" card :lines="4" height="22px" />
    </template>

    <template v-else>
      <UiCard
        v-for="g in view.groups"
        :key="g.group"
        padding="none"
        class="group"
        :class="`group--${g.group.toLowerCase()}`"
      >
        <template #header>
          <div class="group__head">
            <h2 class="group__title">
              <PhWarningCircle
                v-if="g.group === 'OVERDUE'"
                :size="20"
                weight="fill"
                aria-hidden="true"
              />
              {{ groupTitle(g) }}
            </h2>
            <p v-if="g.rows.length" class="group__sum num">
              {{
                $t(
                  'due.summary',
                  { n: g.rows.length, amount: formatMoney(g.estimatedCents) },
                  g.rows.length,
                )
              }}
            </p>
          </div>
        </template>

        <p v-if="!g.rows.length" class="group__empty">
          <PhCheckCircle :size="18" weight="fill" aria-hidden="true" />
          {{
            g.group === 'OVERDUE'
              ? $t('due.overdueEmpty')
              : $t('due.monthEmpty', { month: formatMonth(monthStart(g.month!)) })
          }}
        </p>
        <ul v-else class="rows">
          <li v-for="r in g.rows" :key="r.unit.id" class="row">
            <TypeIcon :type="r.unit.type" />
            <div class="row__who">
              <RouterLink :to="{ name: 'unit', params: { id: r.unit.id } }" class="row__name">{{
                r.unit.customerName
              }}</RouterLink>
              <span class="row__unit">{{ unitName(r.unit) }} · {{ town(r.unit.address) }}</span>
            </div>
            <div class="row__when">
              <span class="row__date num" :class="{ 'is-late': r.unit.daysUntilDue < 0 }">{{
                whenText(r)
              }}</span>
              <UiBadge size="sm" :tone="REMINDER_TONE[r.reminderState]" dot>{{
                stateText(r)
              }}</UiBadge>
            </div>
            <div class="row__actions">
              <UiIconButton
                :icon="PhPhone"
                :label="`${$t('common.call')} ${r.unit.customerName}`"
                size="sm"
                variant="secondary"
                :href="telLink(r.unit.customerPhone) ?? undefined"
              />
              <UiIconButton
                :icon="PhWhatsappLogo"
                :label="`${$t('common.whatsapp')} ${r.unit.customerName}`"
                size="sm"
                variant="secondary"
                :href="r.shareUrl"
                target="_blank"
              />
              <UiButton
                v-if="canRemind(r)"
                size="sm"
                variant="soft"
                :icon="PhBellRinging"
                :loading="sendingId === r.unit.id"
                @click="remind(r)"
              >
                {{ $t('due.remind') }}
              </UiButton>
            </div>
          </li>
        </ul>
      </UiCard>
    </template>
  </AppPage>
</template>

<style scoped>
.monthbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-md), var(--highlight);
}
.monthbar__label {
  min-width: 170px;
  text-align: center;
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 650;
  text-transform: capitalize;
}
.monthbar__back {
  margin-left: auto;
}
.group__head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: 4px 16px;
  width: 100%;
}
.group__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--text-lg);
  text-transform: none;
}
.group--overdue .group__title {
  color: var(--danger-text);
}
.group__sum {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-muted);
}
.group__empty {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px 20px;
  color: var(--success-text);
  font-size: var(--text-sm);
}
.rows {
  margin: 0;
  padding: 0;
  list-style: none;
}
.row {
  display: grid;
  grid-template-columns: auto minmax(0, 1.4fr) minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 12px 20px;
  border-top: 1px solid var(--border);
  transition: background-color var(--duration) var(--ease);
}
.row:hover {
  background: var(--surface-hover);
}
.row__who {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.row__name {
  font-weight: 700;
  color: var(--text);
  text-decoration: none;
}
.row__name:hover {
  color: var(--primary);
  text-decoration: underline;
}
.row__unit {
  font-size: var(--text-sm);
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row__when {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  min-width: 0;
}
.row__date {
  font-size: var(--text-sm);
  font-weight: 650;
}
.row__date.is-late {
  color: var(--danger-text);
}
.row__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}
.hero-menu :deep(.menu__trigger) {
  width: 44px;
  height: 44px;
  color: var(--header-text);
  background: color-mix(in srgb, var(--header-text) 12%, transparent);
  border: 1px solid color-mix(in srgb, var(--header-text) 18%, transparent);
  border-radius: var(--radius-sm);
}
@media (max-width: 760px) {
  /* name across the top; the state and the call buttons share the second line */
  .row {
    grid-template-columns: auto minmax(0, 1fr) auto;
    gap: 8px 12px;
    padding: 14px 16px;
  }
  .row__who {
    grid-column: 2 / -1;
  }
  .row__when {
    grid-column: 2;
  }
  .row__actions {
    grid-column: 3;
    align-self: end;
  }
  .monthbar__label {
    min-width: 0;
    flex: 1;
  }
  .monthbar__back {
    display: none;
  }
}
</style>
