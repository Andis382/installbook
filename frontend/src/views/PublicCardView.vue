<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import {
  PhCalendarCheck,
  PhCalendarPlus,
  PhDownloadSimple,
  PhPaperPlaneTilt,
  PhPhone,
  PhPrinter,
  PhWhatsappLogo,
  PhWrench,
} from '@phosphor-icons/vue'
import PublicLayout from '@/components/layout/PublicLayout.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiEmpty from '@/components/ui/UiEmpty.vue'
import UiField from '@/components/ui/UiField.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiNotice from '@/components/ui/UiNotice.vue'
import UiSegmented from '@/components/ui/UiSegmented.vue'
import UiSkeleton from '@/components/ui/UiSkeleton.vue'
import UiTextarea from '@/components/ui/UiTextarea.vue'
import UiFormErrors from '@/components/ui/UiFormErrors.vue'
import WarrantyCertificate from '@/components/units/WarrantyCertificate.vue'
import { api, ApiError } from '@/lib/api'
import { useForm } from '@/lib/form'
import { addDays } from '@/lib/dates'
import { formatDate, formatDateTime, formatMonth, formatPhone } from '@/lib/format'
import { telLink, waLink } from '@/lib/whatsapp'
import { currentLocale, setLocale, type Locale } from '@/i18n'
import type { Period, PublicCard } from '@/lib/types'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const token = computed(() => String(route.params.token))

const card = ref<PublicCard | null>(null)
const missing = ref(false)
const justBooked = ref(false)
const form = useForm({ preferredDate: '', preferredPeriod: 'ANY' as Period, note: '' })

const periods = computed(() => (['MORNING', 'AFTERNOON', 'ANY'] as Period[]).map((p) => ({ value: p, label: t(`periods.${p}`) })))

async function load() {
  try {
    card.value = await api.get<PublicCard>(`/public/cards/${token.value}`)
    const asked = route.query.lang
    setLocale(asked === 'en' || asked === 'sq' ? asked : card.value.locale)
    form.data.preferredDate = addDays(card.value.today, 3)
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) missing.value = true
    else throw e
  }
}

load()

// Keep the language the customer picks in the link, so a reload or a forward keeps it.
watch(locale, (value) => {
  if (card.value && route.query.lang !== value) router.replace({ query: { ...route.query, lang: value } })
})

const pdfUrl = computed(() => `/api/public/cards/${token.value}/certificate.pdf?lang=${currentLocale() as Locale}`)
const maxDate = computed(() => (card.value ? addDays(card.value.today, 92) : undefined))

async function book() {
  const result = await form.submit(() =>
    api.post<{ created: boolean; booking: PublicCard['openBooking'] }>(`/public/cards/${token.value}/bookings`, {
      preferredDate: form.data.preferredDate,
      preferredPeriod: form.data.preferredPeriod,
      note: form.data.note || null,
    }),
  )
  if (result && card.value) {
    card.value.openBooking = result.booking
    justBooked.value = result.created
  }
}

function print() {
  window.print()
}
</script>

<template>
  <PublicLayout :business="card?.business.name ?? null" :subtitle="card ? $t('card.title') : null">
    <UiCard v-if="missing" padding="lg">
      <UiEmpty :title="$t('card.notFoundTitle')" :text="$t('card.notFoundText')" />
    </UiCard>

    <template v-else-if="!card">
      <UiSkeleton card :lines="7" />
      <UiSkeleton card :lines="4" />
    </template>

    <template v-else>
      <WarrantyCertificate
        :business="card.business.name"
        :customer="card.customerName"
        :type="card.unit.type"
        :brand="card.unit.brand"
        :model="card.unit.model"
        :serial="card.unit.serialNumber"
        :installed-on="card.unit.installedOn"
        :warranty-until="card.unit.warrantyUntil"
        :warranty-active="card.unit.warrantyActive"
        :next-service-due="card.unit.active ? card.unit.nextServiceDue : null"
        :card-number="card.cardNumber"
      />

      <div class="tools no-print">
        <UiButton variant="secondary" :icon="PhDownloadSimple" :href="pdfUrl" download>{{ $t('card.download') }}</UiButton>
        <UiButton variant="ghost" :icon="PhPrinter" @click="print">{{ $t('common.print') }}</UiButton>
      </div>

      <UiNotice v-if="!card.unit.active && card.unit.removedOn" tone="warning">{{ $t('card.removed', { date: formatDate(card.unit.removedOn) }) }}</UiNotice>

      <UiCard v-if="card.unit.active" :title="$t('card.book')" :icon="PhCalendarPlus" class="no-print booking">
        <UiNotice v-if="card.openBooking?.status === 'SCHEDULED' && card.openBooking.scheduledAt" tone="success" :title="$t('card.scheduledTitle')" :icon="PhCalendarCheck">
          {{ $t('card.scheduledText', { when: formatDateTime(card.openBooking.scheduledAt), business: card.business.name }) }}
        </UiNotice>
        <UiNotice v-else-if="card.openBooking" tone="success" :title="justBooked ? $t('card.requestedTitle') : $t('card.alreadyTitle')">
          {{ justBooked ? $t('card.requestedText', { business: card.business.name }) : $t('card.alreadyText', { date: formatDate(card.openBooking.createdAt), business: card.business.name }) }}
        </UiNotice>
        <form v-else class="stack" novalidate @submit.prevent="book">
          <p class="muted small">{{ $t('card.bookText', { business: card.business.name }) }}</p>
          <UiFormErrors :errors="form.errors.value" :message="form.message.value" :trigger="form.submitted.value" />
          <UiField id="f-preferredDate" :label="$t('card.preferredDate')" :error="form.error('preferredDate')" required>
            <template #default="{ id, invalid, describedby }">
              <UiInput :id="id" v-model="form.data.preferredDate" type="date" size="lg" :min="card.today" :max="maxDate" :invalid="invalid" :describedby="describedby" />
            </template>
          </UiField>
          <UiField id="f-preferredPeriod" :label="$t('card.period')">
            <UiSegmented v-model="form.data.preferredPeriod" :options="periods" :label="$t('card.period')" block size="lg" />
          </UiField>
          <UiField id="f-note" :label="$t('card.note')" :error="form.error('note')" optional>
            <template #default="{ id, invalid, describedby }">
              <UiTextarea :id="id" v-model="form.data.note" :rows="2" :placeholder="$t('card.notePlaceholder')" maxlength="500" :invalid="invalid" :describedby="describedby" />
            </template>
          </UiField>
          <UiButton type="submit" size="lg" block :icon="PhPaperPlaneTilt" :loading="form.processing.value">{{ $t('card.send') }}</UiButton>
        </form>
      </UiCard>

      <UiCard :title="$t('card.history')" :icon="PhWrench">
        <dl class="service">
          <div>
            <dt>{{ $t('card.lastService') }}</dt>
            <dd>{{ card.unit.lastServiceOn ? formatDate(card.unit.lastServiceOn) : $t('card.notYet') }}</dd>
          </div>
          <div v-if="card.unit.active">
            <dt>{{ $t('card.nextService') }}</dt>
            <dd>{{ formatMonth(card.unit.nextServiceDue) }}</dd>
          </div>
          <div>
            <dt>{{ $t('card.serviceEvery') }}</dt>
            <dd>{{ $t('common.months', { n: card.unit.serviceIntervalMonths }, card.unit.serviceIntervalMonths) }}</dd>
          </div>
        </dl>
        <p v-if="!card.history.length" class="muted small history-empty">{{ $t('card.historyEmpty', { date: formatMonth(card.unit.nextServiceDue) }) }}</p>
        <ol v-else class="history">
          <li v-for="(v, i) in card.history" :key="i">
            <span class="history__date num">{{ formatDate(v.date) }}</span>
            <span class="history__what">
              <strong>{{ $t(`visitKinds.${v.kind}`) }}</strong>
              <span v-if="v.parts" class="muted">{{ v.parts }}</span>
            </span>
          </li>
        </ol>
      </UiCard>

      <UiCard class="contact">
        <div class="contact__row">
          <div>
            <p class="eyebrow">{{ $t('card.installedBy') }}</p>
            <p class="contact__name">{{ card.business.name }}</p>
            <p v-if="card.business.phone" class="muted num">{{ formatPhone(card.business.phone) }}</p>
          </div>
          <div v-if="card.business.phone" class="contact__actions no-print">
            <UiButton :icon="PhPhone" :href="telLink(card.business.phone) ?? undefined">{{ $t('common.call') }}</UiButton>
            <UiButton variant="secondary" :icon="PhWhatsappLogo" :href="waLink(card.business.phone) ?? undefined" target="_blank">{{ $t('common.whatsapp') }}</UiButton>
          </div>
        </div>
        <p class="small muted">{{ $t('card.help', { business: card.business.name }) }}</p>
      </UiCard>
    </template>
  </PublicLayout>
</template>

<style scoped>
.tools {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.service {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 0 0 16px;
  padding: 12px 14px;
  border-radius: var(--radius);
  background: var(--surface-muted);
  border: 1px solid var(--border);
}
.service dt {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-subtle);
}
.service dd {
  margin: 2px 0 0;
  font-weight: 650;
}
.history {
  margin: 0;
  padding: 0;
  list-style: none;
}
.history li {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 12px;
  padding: 10px 0;
  border-top: 1px solid var(--border);
  font-size: var(--text-sm);
}
.history__date {
  color: var(--text-muted);
}
.history__what {
  display: flex;
  flex-direction: column;
}
.contact__row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}
.contact__name {
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 700;
}
.contact__actions {
  display: flex;
  gap: 8px;
}
@media (max-width: 520px) {
  .service {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }
  .history li {
    grid-template-columns: minmax(0, 1fr);
    gap: 2px;
  }
  .contact__actions {
    width: 100%;
  }
  .contact__actions :deep(.btn) {
    flex: 1;
  }
}
@media print {
  .no-print {
    display: none !important;
  }
}
</style>

<style>
@media print {
  body {
    background: var(--surface);
  }
  .public__band {
    padding-bottom: 12px !important;
    background: none !important;
    color: var(--text) !important;
  }
  .public__band .lang,
  .public__foot {
    display: none !important;
  }
  .public__main {
    margin-top: 0 !important;
  }
  .card {
    box-shadow: none !important;
    break-inside: avoid;
  }
}
</style>
