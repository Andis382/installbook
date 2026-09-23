<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { PhBarcode, PhChatsCircle, PhCheck, PhIdentificationCard, PhPencilSimple, PhPhone, PhWhatsappLogo } from '@phosphor-icons/vue'
import AppPage from '@/components/layout/AppPage.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiEmpty from '@/components/ui/UiEmpty.vue'
import UiField from '@/components/ui/UiField.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiSegmented from '@/components/ui/UiSegmented.vue'
import UiSkeleton from '@/components/ui/UiSkeleton.vue'
import UiSwitch from '@/components/ui/UiSwitch.vue'
import UiTextarea from '@/components/ui/UiTextarea.vue'
import UiFormErrors from '@/components/ui/UiFormErrors.vue'
import UnitLine from '@/components/units/UnitLine.vue'
import { api } from '@/lib/api'
import { useForm } from '@/lib/form'
import { formatDate, formatPhone, formatStamp } from '@/lib/format'
import { telLink, waLink } from '@/lib/whatsapp'
import type { CustomerDetail } from '@/lib/types'
import { useConfirm } from '@/stores/confirm'
import { useToasts } from '@/stores/toasts'

const { t } = useI18n()
const route = useRoute()
const toasts = useToasts()
const confirm = useConfirm()

const customer = ref<CustomerDetail | null>(null)
const editing = ref(false)
const savingConsent = ref(false)
const form = useForm({ name: '', phone: '', locale: 'sq' as 'sq' | 'en', notes: '' })
const localeOptions = computed(() => [
  { value: 'sq' as const, label: t('common.languages.sq') },
  { value: 'en' as const, label: t('common.languages.en') },
])

async function load() {
  customer.value = await api.get<CustomerDetail>(`/customers/${route.params.id}`)
}

watch(() => route.params.id, load, { immediate: true })

function edit() {
  const c = customer.value
  if (!c) return
  form.reset({ name: c.name, phone: formatPhone(c.phone), locale: c.locale, notes: c.notes ?? '' })
  editing.value = true
}

async function save() {
  const saved = await form.submit(() => api.put<CustomerDetail>(`/customers/${route.params.id}`, form.data))
  if (saved) {
    customer.value = saved
    editing.value = false
    toasts.success(t('customer.saved'))
  }
}

const consent = computed({
  get: () => !!customer.value?.whatsappOptIn,
  set: (value: boolean) => setConsent(value),
})

async function setConsent(optIn: boolean) {
  const c = customer.value
  if (!c) return
  if (optIn) {
    const ok = await confirm.ask({
      title: t('customer.consentAskTitle', { name: c.name }),
      text: t('customer.consentAskText'),
      confirmLabel: t('customer.consentAskConfirm'),
    })
    if (!ok) return
  }
  savingConsent.value = true
  try {
    customer.value = await api.put<CustomerDetail>(`/customers/${c.id}/consent`, { optIn })
    toasts.success(t('customer.consentSaved'))
  } finally {
    savingConsent.value = false
  }
}

const consentText = computed(() => {
  const c = customer.value
  if (!c) return ''
  if (c.whatsappOptIn) return t('customer.consentOn', { date: formatDate(c.whatsappOptInAt) })
  if (c.whatsappOptOutAt) return t('customer.consentStopped', { date: formatDate(c.whatsappOptOutAt) })
  return t('customer.consentOff')
})
</script>

<template>
  <AppPage
    :title="customer?.name ?? $t('common.loading')"
    :subtitle="customer ? `${formatPhone(customer.phone)} · ${$t('customer.since', { date: formatDate(customer.createdAt) })}` : undefined"
    :back="{ name: 'customers' }"
    :back-label="$t('nav.customers')"
  >
    <template v-if="customer" #actions>
      <UiButton variant="inverse" :icon="PhPhone" :href="telLink(customer.phone) ?? undefined">{{ $t('common.call') }}</UiButton>
      <UiButton variant="inverse" :icon="PhWhatsappLogo" :href="waLink(customer.phone) ?? undefined" target="_blank">{{ $t('common.whatsapp') }}</UiButton>
    </template>

    <div v-if="!customer" class="grid-2"><UiSkeleton card :lines="5" /><UiSkeleton card :lines="7" /></div>

    <div v-else class="layout">
      <div class="layout__side">
        <UiCard :title="$t('customer.contact')" :icon="PhIdentificationCard">
          <template v-if="!editing" #actions>
            <UiButton size="sm" variant="ghost" :icon="PhPencilSimple" @click="edit">{{ $t('common.edit') }}</UiButton>
          </template>
          <form v-if="editing" class="stack" novalidate @submit.prevent="save">
            <UiFormErrors :errors="form.errors.value" :message="form.message.value" :trigger="form.submitted.value" />
            <UiField id="f-name" :label="$t('common.name')" :error="form.error('name')" required>
              <template #default="{ id, invalid, describedby }">
                <UiInput :id="id" v-model="form.data.name" :invalid="invalid" :describedby="describedby" />
              </template>
            </UiField>
            <UiField id="f-phone" :label="$t('common.phone')" :error="form.error('phone')" required>
              <template #default="{ id, invalid, describedby }">
                <UiInput :id="id" v-model="form.data.phone" type="tel" inputmode="tel" :invalid="invalid" :describedby="describedby" />
              </template>
            </UiField>
            <UiField id="f-locale" :label="$t('install.language')">
              <UiSegmented v-model="form.data.locale" :options="localeOptions" :label="$t('install.language')" />
            </UiField>
            <UiField id="f-notes" :label="$t('customer.notes')" :error="form.error('notes')" optional>
              <template #default="{ id, invalid, describedby }">
                <UiTextarea :id="id" v-model="form.data.notes" :rows="3" :invalid="invalid" :describedby="describedby" />
              </template>
            </UiField>
            <div class="cluster">
              <UiButton type="submit" :icon="PhCheck" :loading="form.processing.value">{{ $t('common.save') }}</UiButton>
              <UiButton variant="ghost" @click="editing = false">{{ $t('common.cancel') }}</UiButton>
            </div>
          </form>
          <dl v-else class="facts">
            <div>
              <dt>{{ $t('common.phone') }}</dt>
              <dd class="num">{{ formatPhone(customer.phone) }}</dd>
            </div>
            <div>
              <dt>{{ $t('customer.language') }}</dt>
              <dd>{{ $t(`common.languages.${customer.locale}`) }}</dd>
            </div>
            <div class="facts__wide">
              <dt>{{ $t('customer.notes') }}</dt>
              <dd class="muted">{{ customer.notes || $t('customer.notesEmpty') }}</dd>
            </div>
          </dl>
        </UiCard>

        <UiCard :title="$t('customer.consent')" :icon="PhWhatsappLogo" :tone="customer.whatsappOptIn ? 'default' : 'muted'">
          <UiSwitch v-model="consent" :label="consentText" :disabled="savingConsent" />
        </UiCard>

        <UiCard :title="$t('customer.units')" :icon="PhBarcode">
          <p v-if="!customer.units.length" class="muted small">{{ $t('customer.unitsEmpty') }}</p>
          <UnitLine v-for="u in customer.units" :key="u.id" :unit="u" :show-customer="false" />
        </UiCard>
      </div>

      <UiCard :title="$t('customer.conversation')" :icon="PhChatsCircle" class="layout__main">
        <UiEmpty v-if="!customer.conversation.length" :icon="PhChatsCircle" :title="$t('customer.conversationEmpty')" compact />
        <ol v-else class="chat">
          <li v-for="(m, i) in customer.conversation" :key="i" class="chat__msg" :class="m.direction === 'OUT' ? 'chat__msg--out' : 'chat__msg--in'">
            <p class="chat__body">{{ m.body }}</p>
            <p class="chat__meta num">
              {{ m.direction === 'OUT' ? $t('customer.sent') : $t('customer.received') }} · {{ formatStamp(m.at) }}
              <template v-if="m.direction === 'OUT'"> · {{ $t(`messages.status.${m.status}`) }}</template>
            </p>
          </li>
        </ol>
      </UiCard>
    </div>
  </AppPage>
</template>

<style scoped>
.layout {
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.2fr);
  align-items: start;
}
.layout__side {
  display: flex;
  flex-direction: column;
  gap: 20px;
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
}
.facts__wide {
  grid-column: 1 / -1;
}
.chat {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
  max-height: 720px;
  overflow-y: auto;
}
.chat__msg {
  max-width: 88%;
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid var(--border);
}
.chat__msg--out {
  align-self: flex-end;
  border-top-right-radius: 4px;
  background: var(--success-soft);
  border-color: color-mix(in srgb, var(--success) 16%, transparent);
}
.chat__msg--in {
  align-self: flex-start;
  border-top-left-radius: 4px;
  background: var(--surface-muted);
}
.chat__body {
  font-size: var(--text-sm);
  white-space: pre-line;
  overflow-wrap: anywhere;
}
.chat__meta {
  margin-top: 6px;
  font-size: 11px;
  color: var(--text-subtle);
}
@media (max-width: 980px) {
  .layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
