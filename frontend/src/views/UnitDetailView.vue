<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  PhArrowCounterClockwise,
  PhArrowSquareOut,
  PhBellRinging,
  PhCalendarPlus,
  PhCamera,
  PhClockCounterClockwise,
  PhIdentificationCard,
  PhMapPin,
  PhPaperPlaneTilt,
  PhPencilSimple,
  PhPhone,
  PhPlugs,
  PhWhatsappLogo,
  PhWrench,
} from '@phosphor-icons/vue'
import AppPage from '@/components/layout/AppPage.vue'
import UiBadge from '@/components/ui/UiBadge.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiEmpty from '@/components/ui/UiEmpty.vue'
import UiMenu, { type MenuItem } from '@/components/ui/UiMenu.vue'
import UiNotice from '@/components/ui/UiNotice.vue'
import UiSkeleton from '@/components/ui/UiSkeleton.vue'
import SerialChip from '@/components/units/SerialChip.vue'
import UnitLine from '@/components/units/UnitLine.vue'
import UnitPills from '@/components/units/UnitPills.vue'
import UnitTimeline from '@/components/units/UnitTimeline.vue'
import VisitDialog from '@/components/units/VisitDialog.vue'
import { api, ApiError } from '@/lib/api'
import { formatDate, formatDateTime, formatPhone, formatRelative, todayIso } from '@/lib/format'
import { mapUrl, unitName } from '@/lib/units'
import { telLink, waLink } from '@/lib/whatsapp'
import type { Message, UnitDetail } from '@/lib/types'
import { useConfirm } from '@/stores/confirm'
import { useToasts } from '@/stores/toasts'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const toasts = useToasts()
const confirm = useConfirm()

const unit = ref<UnitDetail | null>(null)
const missing = ref(false)
const visitOpen = ref(false)
const sending = ref(false)

async function load() {
  try {
    unit.value = await api.get<UnitDetail>(`/units/${route.params.id}`)
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) missing.value = true
    else throw e
  }
}

watch(() => route.params.id, load, { immediate: true })

const optedIn = computed(() => !!unit.value?.customer.whatsappOptIn)
const active = computed(() => unit.value?.status === 'ACTIVE')

const reminderText = computed(() => {
  const u = unit.value
  if (!u) return ''
  if (u.openBooking?.status === 'SCHEDULED' && u.openBooking.scheduledAt) return t('unit.scheduledBooking', { when: formatDateTime(u.openBooking.scheduledAt) })
  if (u.openBooking) return t('unit.openBooking', { when: formatRelative(u.openBooking.createdAt) })
  const r = u.cycleReminder
  if (r.outcome === 'NO_CONSENT') return t('reminder.NO_CONSENT')
  if (r.outcome === 'FAILED') return t('reminder.FAILED')
  if (r.sentAt) return t('reminder.SENT', { date: formatDate(r.sentAt) })
  return r.goesOutOn > todayIso() ? t('reminder.PLANNED', { date: formatDate(r.goesOutOn) }) : t('reminder.WAITING')
})

function delivered(m: Message | null) {
  return !!m && (m.status === 'SENT' || m.status === 'DELIVERED' || m.status === 'READ')
}

async function sendReminder() {
  if (!unit.value) return
  sending.value = true
  try {
    const result = await api.post<{ message: Message | null }>(`/units/${unit.value.id}/remind`)
    toasts.success(delivered(result.message) ? t('unit.reminderSent') : t('unit.reminderOutbox'))
    await load()
  } catch (e) {
    toasts.error(e instanceof ApiError && e.status ? e.message : t('errors.generic'))
  } finally {
    sending.value = false
  }
}

async function resendCard() {
  if (!unit.value) return
  try {
    const result = await api.post<{ message: Message | null }>(`/units/${unit.value.id}/card`)
    toasts.success(delivered(result.message) ? t('unit.cardSent') : t('unit.cardOutbox'))
    await load()
  } catch (e) {
    toasts.error(e instanceof ApiError && e.status ? e.message : t('errors.generic'))
  }
}

async function remove() {
  if (!unit.value) return
  const ok = await confirm.ask({ title: t('unit.removeTitle'), text: t('unit.removeText'), confirmLabel: t('unit.remove'), danger: true })
  if (!ok) return
  unit.value = await api.post<UnitDetail>(`/units/${unit.value.id}/remove`)
  toasts.success(t('unit.removedToast'))
}

async function restore() {
  if (!unit.value) return
  try {
    unit.value = await api.post<UnitDetail>(`/units/${unit.value.id}/restore`)
    toasts.success(t('unit.restoredToast'))
  } catch (e) {
    toasts.error(e instanceof ApiError && e.status ? e.message : t('errors.generic'))
  }
}

function openUrl(url: string) {
  window.open(url, '_blank', 'noopener')
}

const menu = computed<MenuItem[]>(() => {
  const u = unit.value
  if (!u) return []
  const items: MenuItem[] = []
  if (active.value) {
    items.push(
      optedIn.value
        ? { label: t('unit.resendCard'), icon: PhPaperPlaneTilt, action: resendCard }
        : { label: t('unit.shareCard'), icon: PhWhatsappLogo, action: () => openUrl(u.cardShareUrl) },
    )
  }
  items.push(
    { label: t('unit.openCard'), icon: PhArrowSquareOut, action: () => openUrl(u.cardUrl) },
    { label: t('unit.edit'), icon: PhPencilSimple, action: () => router.push({ name: 'unit-edit', params: { id: u.id } }) },
    active.value
      ? { label: t('unit.remove'), icon: PhPlugs, danger: true, action: remove }
      : { label: t('unit.restore'), icon: PhArrowCounterClockwise, action: restore },
  )
  return items
})
</script>

<template>
  <AppPage v-if="missing" :title="$t('errors.notFoundTitle')" :back="{ name: 'units' }" :back-label="$t('nav.units')">
    <UiCard><UiEmpty :title="$t('errors.notFoundTitle')" :text="$t('errors.notFoundText')" /></UiCard>
  </AppPage>

  <AppPage
    v-else
    :title="unit ? unitName(unit) : $t('common.loading')"
    :eyebrow="unit ? $t('unit.eyebrow', { type: $t(`types.${unit.type}`), date: formatDate(unit.installedOn) }) : undefined"
    :back="{ name: 'units' }"
    :back-label="$t('nav.units')"
  >
    <template v-if="unit" #meta>
      <SerialChip :serial="unit.serialNumber" size="lg" copyable />
      <UnitPills
        :warranty-active="unit.warrantyActive"
        :warranty-until="unit.warrantyUntil"
        :service-state="unit.serviceState"
        :days-until-due="unit.daysUntilDue"
        :next-service-due="unit.nextServiceDue"
        :status="unit.status"
      />
    </template>
    <template v-if="unit" #actions>
      <UiButton v-if="active" :icon="PhWrench" @click="visitOpen = true">{{ $t('unit.recordVisit') }}</UiButton>
      <UiButton v-if="active && optedIn" variant="inverse" :icon="PhBellRinging" :loading="sending" @click="sendReminder">{{ $t('unit.sendReminder') }}</UiButton>
      <UiButton v-else-if="active" variant="inverse" :icon="PhWhatsappLogo" :href="unit.reminderShareUrl" target="_blank">{{ $t('unit.sendReminder') }}</UiButton>
      <UiMenu class="hero-menu" :items="menu" :label="$t('common.more')" />
    </template>

    <template v-if="!unit">
      <div class="grid-2"><UiSkeleton card :lines="6" /><UiSkeleton card :lines="8" /></div>
    </template>

    <template v-else>
      <UiNotice v-if="!active && unit.removedOn" tone="warning" :icon="PhPlugs">
        {{ $t('unit.removedNotice', { date: formatDate(unit.removedOn) }) }}
        <template #actions>
          <UiButton size="sm" variant="secondary" :icon="PhArrowCounterClockwise" @click="restore">{{ $t('unit.restore') }}</UiButton>
        </template>
      </UiNotice>
      <UiNotice v-if="unit.openBooking && active" tone="info" :icon="PhCalendarPlus">
        {{ reminderText }}
        <template #actions>
          <UiButton size="sm" variant="secondary" :to="{ name: 'bookings' }">{{ $t('nav.bookings') }}</UiButton>
        </template>
      </UiNotice>

      <div class="detail">
        <div class="detail__side">
          <UiCard :title="$t('unit.customer')" :icon="PhIdentificationCard" class="order-1">
            <div class="who">
              <RouterLink :to="{ name: 'customer', params: { id: unit.customer.id } }" class="who__name">{{ unit.customer.name }}</RouterLink>
              <p class="who__phone num">{{ formatPhone(unit.customer.phone) }}</p>
              <UiBadge :tone="optedIn ? 'success' : 'warning'" size="sm" :icon="PhWhatsappLogo">
                {{ optedIn ? $t('customer.consentOn', { date: formatDate(unit.customer.whatsappOptInAt) }) : $t('customer.consentOff') }}
              </UiBadge>
              <div class="who__actions">
                <UiButton size="sm" variant="secondary" :icon="PhPhone" :href="telLink(unit.customer.phone) ?? undefined">{{ $t('common.call') }}</UiButton>
                <UiButton size="sm" variant="secondary" :icon="PhWhatsappLogo" :href="waLink(unit.customer.phone) ?? undefined" target="_blank">{{ $t('common.whatsapp') }}</UiButton>
              </div>
              <div class="who__address">
                <PhMapPin :size="18" weight="duotone" aria-hidden="true" />
                <span>{{ unit.address }}</span>
                <a :href="mapUrl(unit)" target="_blank" rel="noopener" class="who__map">{{ $t('unit.openMap') }}</a>
              </div>
            </div>
          </UiCard>

          <UiCard :title="$t('unit.facts')" :icon="PhClockCounterClockwise" class="order-2">
            <dl class="facts">
              <div>
                <dt>{{ $t('unit.installed') }}</dt>
                <dd>{{ unit.installedByName ? $t('unit.installedBy', { date: formatDate(unit.installedOn), name: unit.installedByName }) : formatDate(unit.installedOn) }}</dd>
              </div>
              <div>
                <dt>{{ $t('unit.warranty') }}</dt>
                <dd>
                  {{
                    $t(unit.warrantyActive ? 'unit.warrantyTerm' : 'unit.warrantyTermEnded', {
                      months: $t('common.months', { n: unit.warrantyMonths }, unit.warrantyMonths),
                      date: formatDate(unit.warrantyUntil),
                    })
                  }}
                </dd>
              </div>
              <div>
                <dt>{{ $t('unit.serviceEvery') }}</dt>
                <dd>{{ $t('common.months', { n: unit.serviceIntervalMonths }, unit.serviceIntervalMonths) }}</dd>
              </div>
              <div>
                <dt>{{ $t('unit.lastService') }}</dt>
                <dd>{{ unit.lastServiceOn ? formatDate(unit.lastServiceOn) : $t('unit.never') }}</dd>
              </div>
              <div v-if="active">
                <dt>{{ $t('unit.nextService') }}</dt>
                <dd class="strong">{{ formatDate(unit.nextServiceDue) }}</dd>
              </div>
              <div v-if="active">
                <dt>{{ $t('unit.reminder') }}</dt>
                <dd>{{ reminderText }}</dd>
              </div>
              <div v-if="unit.notes" class="facts__wide">
                <dt>{{ $t('unit.notes') }}</dt>
                <dd class="facts__notes">{{ unit.notes }}</dd>
              </div>
            </dl>
          </UiCard>

          <UiCard :title="$t('unit.plate')" :icon="PhCamera" padding="sm" class="order-4">
            <a v-if="unit.platePhotoUrl" :href="unit.platePhotoUrl" target="_blank" rel="noopener" class="plate-photo">
              <img :src="unit.platePhotoUrl" :alt="$t('unit.plate')" loading="lazy" />
            </a>
            <UiEmpty v-else :icon="PhCamera" :title="$t('unit.noPhoto')" :text="$t('unit.noPhotoText')" compact />
          </UiCard>

          <UiCard v-if="unit.otherUnits.length" :title="$t('unit.otherUnits')" class="order-5">
            <UnitLine v-for="u in unit.otherUnits" :key="u.id" :unit="u" :show-customer="false" />
          </UiCard>
        </div>

        <UiCard :title="$t('unit.history')" :icon="PhClockCounterClockwise" class="detail__main order-3">
          <UnitTimeline :entries="unit.timeline" />
        </UiCard>
      </div>

      <VisitDialog
        v-model:open="visitOpen"
        :unit-id="unit.id"
        :unit-label="unitName(unit)"
        :customer-name="unit.customer.name"
        @saved="load"
      />
    </template>
  </AppPage>
</template>

<style scoped>
.detail {
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.15fr);
  align-items: start;
}
.detail__side {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.who {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}
.who__name {
  font-family: var(--font-display);
  font-size: var(--text-xl);
  font-weight: 650;
  color: var(--text);
  text-decoration: none;
}
.who__name:hover {
  color: var(--primary);
}
.who__phone {
  color: var(--text-muted);
  margin-top: -6px;
}
.who__actions {
  display: flex;
  gap: 8px;
}
.who__address {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
  margin-top: 4px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
  font-size: var(--text-sm);
}
.who__address :deep(svg) {
  flex: none;
  color: var(--primary);
}
.who__address span {
  flex: 1;
}
.who__map {
  flex: none;
  font-weight: 650;
}
.facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 18px;
  margin: 0;
}
.facts dt {
  font-size: var(--text-xs);
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-subtle);
}
.facts dd {
  margin: 2px 0 0;
  font-size: var(--text-sm);
  font-variant-numeric: tabular-nums;
}
.facts__wide {
  grid-column: 1 / -1;
}
.facts__notes {
  white-space: pre-line;
  color: var(--text-muted);
}
.plate-photo {
  display: block;
  overflow: hidden;
  border-radius: var(--radius);
  border: 1px solid var(--border);
  background: var(--surface-sunken);
}
.plate-photo img {
  width: 100%;
  aspect-ratio: 72 / 47;
  object-fit: cover;
  transition: transform 300ms var(--ease);
}
.plate-photo:hover img {
  transform: scale(1.02);
}
.hero-menu :deep(.menu__trigger) {
  width: 44px;
  height: 44px;
  color: var(--header-text);
  background: color-mix(in srgb, var(--header-text) 12%, transparent);
  border: 1px solid color-mix(in srgb, var(--header-text) 18%, transparent);
  border-radius: var(--radius-sm);
}
@media (max-width: 980px) {
  /* one column: the history moves up, right after the facts, before the photo */
  .detail {
    display: flex;
    flex-direction: column;
  }
  .detail__side {
    display: contents;
  }
  .order-1 {
    order: 1;
  }
  .order-2 {
    order: 2;
  }
  .order-3 {
    order: 3;
  }
  .order-4 {
    order: 4;
  }
  .order-5 {
    order: 5;
  }
}
@media (max-width: 420px) {
  .facts {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
