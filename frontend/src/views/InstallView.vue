<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { PhCheckCircle, PhCrosshairSimple, PhMapPin, PhPaperPlaneTilt, PhScan, PhUserCircle } from '@phosphor-icons/vue'
import AppPage from '@/components/layout/AppPage.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiField from '@/components/ui/UiField.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiTextarea from '@/components/ui/UiTextarea.vue'
import UiSegmented from '@/components/ui/UiSegmented.vue'
import UiStepper from '@/components/ui/UiStepper.vue'
import UiCheckbox from '@/components/ui/UiCheckbox.vue'
import UiNotice from '@/components/ui/UiNotice.vue'
import UiPhotoInput from '@/components/ui/UiPhotoInput.vue'
import UiFormErrors from '@/components/ui/UiFormErrors.vue'
import TypePicker from '@/components/units/TypePicker.vue'
import UnitLine from '@/components/units/UnitLine.vue'
import { api, ApiError, query } from '@/lib/api'
import { useForm } from '@/lib/form'
import { formatDate, formatMonth, todayIso } from '@/lib/format'
import { nextServiceDue, warrantyUntil } from '@/lib/dates'
import { WARRANTY_CHIPS } from '@/lib/units'
import type { CustomerLookup, InstallerSettings, InstallResult, PlateReading, UnitType } from '@/lib/types'
import { useToasts } from '@/stores/toasts'

const { t } = useI18n()
const router = useRouter()
const toasts = useToasts()

const form = useForm({
  type: null as UnitType | null,
  brand: '',
  model: '',
  serialNumber: '',
  customerPhone: '',
  customerName: '',
  customerLocale: 'sq' as 'sq' | 'en',
  whatsappConsent: false,
  address: '',
  latitude: null as number | null,
  longitude: null as number | null,
  installedOn: todayIso(),
  warrantyMonths: 24 as number | null,
  serviceIntervalMonths: 12 as number | null,
  notes: '',
})

const brands = ref<string[]>([])
const photo = ref<File | null>(null)
const photoId = ref<string | null>(null)
const plate = ref<'idle' | 'reading' | 'filled' | 'off' | 'nothing'>('idle')
const plateNotes = ref<string | null>(null)
const lookup = ref<CustomerLookup | null>(null)
const locating = ref(false)
const location = ref<'saved' | 'noAddress' | 'failed' | null>(null)
const warrantyChoice = ref<number | 'custom'>(24)

const localeOptions = computed(() => [
  { value: 'sq' as const, label: t('common.languages.sq') },
  { value: 'en' as const, label: t('common.languages.en') },
])
const warrantyOptions = computed(() => [
  ...WARRANTY_CHIPS.map((m) => ({ value: m as number | 'custom', label: t('common.years', { n: m / 12 }, m / 12) })),
  { value: 'custom' as number | 'custom', label: t('install.warrantyOther') },
])

onMounted(async () => {
  const [s, b] = await Promise.all([api.get<InstallerSettings>('/settings/installer'), api.get<string[]>('/units/brands')])
  brands.value = b
  form.data.serviceIntervalMonths = s.defaultServiceIntervalMonths
  form.data.warrantyMonths = s.defaultWarrantyMonths
  warrantyChoice.value = WARRANTY_CHIPS.includes(s.defaultWarrantyMonths) ? s.defaultWarrantyMonths : 'custom'
})

watch(warrantyChoice, (choice) => {
  if (choice !== 'custom') form.data.warrantyMonths = choice
})

watch(photo, () => {
  photoId.value = null
  plate.value = 'idle'
  plateNotes.value = null
})

async function upload(read: boolean): Promise<PlateReading> {
  const data = new FormData()
  data.append('file', photo.value as File)
  const result = await api.upload<PlateReading>(`/units/plate${query({ read })}`, data)
  photoId.value = result.photoId
  return result
}

async function readPlate() {
  if (!photo.value) return
  plate.value = 'reading'
  try {
    const result = await upload(true)
    const r = result.reading
    if (!result.aiEnabled) {
      plate.value = 'off'
    } else if (!r) {
      plate.value = 'nothing'
    } else {
      if (r.type) form.data.type = r.type
      if (r.brand) form.data.brand = r.brand
      if (r.model) form.data.model = r.model
      if (r.serialNumber) form.data.serialNumber = r.serialNumber
      plateNotes.value = r.notes
      plate.value = 'filled'
    }
  } catch (e) {
    plate.value = 'idle'
    toasts.error(e instanceof ApiError && e.status ? e.message : t('errors.network'))
  }
}

let lookupTimer: ReturnType<typeof setTimeout> | undefined
watch(
  () => form.data.customerPhone,
  (phone) => {
    clearTimeout(lookupTimer)
    if (phone.replace(/\D/g, '').length < 8) {
      lookup.value = null
      return
    }
    lookupTimer = setTimeout(() => findCustomer(phone), 350)
  },
)
onBeforeUnmount(() => clearTimeout(lookupTimer))

async function findCustomer(phone: string) {
  const result = await api.get<CustomerLookup>(`/customers/lookup${query({ phone })}`)
  if (form.data.customerPhone !== phone) return
  lookup.value = result
  if (result.found && result.customer) {
    form.data.customerName = result.customer.name
    form.data.customerLocale = result.customer.locale
  }
}

const consentOnFile = computed(() => !!lookup.value?.customer?.whatsappOptIn)
const willSend = computed(() => form.data.whatsappConsent || consentOnFile.value)

function locate() {
  if (!('geolocation' in navigator)) {
    location.value = 'failed'
    return
  }
  locating.value = true
  location.value = null
  navigator.geolocation.getCurrentPosition(
    async (pos) => {
      form.data.latitude = Number(pos.coords.latitude.toFixed(6))
      form.data.longitude = Number(pos.coords.longitude.toFixed(6))
      try {
        const place = await api.get<{ address: string | null }>(`/geo/reverse${query({ lat: form.data.latitude, lng: form.data.longitude })}`)
        if (place.address) form.data.address = place.address
        location.value = place.address ? 'saved' : 'noAddress'
      } catch {
        location.value = 'noAddress'
      } finally {
        locating.value = false
      }
    },
    () => {
      locating.value = false
      location.value = 'failed'
    },
    { enableHighAccuracy: true, timeout: 12000, maximumAge: 60000 },
  )
}

const preview = computed(() => {
  const months = form.data.warrantyMonths
  const interval = form.data.serviceIntervalMonths
  if (!form.data.installedOn || !months || !interval) return null
  return t('install.preview', {
    until: formatDate(warrantyUntil(form.data.installedOn, months)),
    due: formatMonth(nextServiceDue(form.data.installedOn, null, interval)),
  })
})

function tidySerial() {
  form.data.serialNumber = form.data.serialNumber.trim().replace(/\s+/g, ' ').toUpperCase()
}

async function save() {
  const result = await form.submit(async () => {
    if (photo.value && !photoId.value) await upload(false)
    return api.post<InstallResult>('/units', { ...form.data, platePhotoId: photoId.value })
  })
  if (result) {
    toasts.success(t('install.saved'))
    await router.push({ name: 'install-done', params: { id: result.unit.id } })
  }
}
</script>

<template>
  <AppPage :title="$t('install.title')" :subtitle="$t('install.subtitle')">
    <form class="install" novalidate @submit.prevent="save">
      <UiFormErrors class="span-all" :errors="form.errors.value" :message="form.message.value" :trigger="form.submitted.value" />

      <UiCard class="install__unit">
        <template #header>
          <div class="step"><span class="step__n">1</span><div><h2 class="step__title">{{ $t('install.unitStep') }}</h2><p class="step__hint">{{ $t('install.unitStepHint') }}</p></div></div>
        </template>
        <div class="stack">
          <UiField id="f-file" :error="form.error('file')">
            <UiPhotoInput id="f-file" v-model="photo" aspect="16 / 10" :hint="$t('install.photoHint')" :busy="plate === 'reading'" />
          </UiField>
          <UiButton v-if="photo" variant="soft" :icon="PhScan" :loading="plate === 'reading'" block @click="readPlate">
            {{ plate === 'reading' ? $t('install.reading') : $t('install.readPlate') }}
          </UiButton>
          <UiNotice v-if="plate === 'filled'" tone="warning" :title="$t('install.aiFilledTitle')">
            {{ $t('install.aiFilled') }}<template v-if="plateNotes"> {{ plateNotes }}</template>
          </UiNotice>
          <UiNotice v-else-if="plate === 'off'" tone="info">{{ $t('install.aiOff') }}</UiNotice>
          <UiNotice v-else-if="plate === 'nothing'" tone="info">{{ $t('install.aiNothing') }}</UiNotice>

          <UiField id="f-type" :label="$t('install.type')" :error="form.error('type')" required>
            <TypePicker id="f-type" v-model="form.data.type" :label="$t('install.type')" :invalid="!!form.error('type')" />
          </UiField>
          <div class="grid-2">
            <UiField id="f-brand" :label="$t('install.brand')" :error="form.error('brand')" required>
              <template #default="{ id, invalid, describedby }">
                <UiInput :id="id" v-model="form.data.brand" list="brand-list" autocomplete="off" :invalid="invalid" :describedby="describedby" />
              </template>
            </UiField>
            <UiField id="f-model" :label="$t('install.model')" :error="form.error('model')" optional>
              <template #default="{ id, invalid, describedby }">
                <UiInput :id="id" v-model="form.data.model" autocomplete="off" :invalid="invalid" :describedby="describedby" />
              </template>
            </UiField>
          </div>
          <datalist id="brand-list">
            <option v-for="b in brands" :key="b" :value="b" />
          </datalist>
          <UiField id="f-serialNumber" :label="$t('install.serial')" :hint="$t('install.serialHint')" :error="form.error('serialNumber')">
            <template #default="{ id, invalid, describedby }">
              <UiInput
                :id="id"
                v-model="form.data.serialNumber"
                mono
                size="lg"
                autocomplete="off"
                autocapitalize="characters"
                spellcheck="false"
                :invalid="invalid"
                :describedby="describedby"
                @blur="tidySerial"
              />
            </template>
          </UiField>
        </div>
      </UiCard>

      <div class="install__side">
        <UiCard>
          <template #header>
            <div class="step"><span class="step__n">2</span><div><h2 class="step__title">{{ $t('install.customerStep') }}</h2><p class="step__hint">{{ $t('install.customerStepHint') }}</p></div></div>
          </template>
          <div class="stack">
            <UiField id="f-customerPhone" :label="$t('install.phone')" :hint="$t('install.phoneHint')" :error="form.error('customerPhone')" required>
              <template #default="{ id, invalid, describedby }">
                <UiInput :id="id" v-model="form.data.customerPhone" type="tel" inputmode="tel" autocomplete="off" size="lg" placeholder="069 123 4567" :invalid="invalid" :describedby="describedby" />
              </template>
            </UiField>
            <div v-if="lookup?.found && lookup.customer" class="returning">
              <p class="returning__title"><PhUserCircle :size="18" weight="fill" aria-hidden="true" /> {{ $t('install.returningTitle') }}</p>
              <p class="small muted">{{ $t('install.returningText', { name: lookup.customer.name }) }}</p>
              <div v-if="lookup.units.length" class="returning__units">
                <p class="eyebrow">{{ $t('install.theirUnits') }}</p>
                <UnitLine v-for="u in lookup.units" :key="u.id" :unit="u" :show-customer="false" />
              </div>
            </div>
            <UiField id="f-customerName" :label="$t('install.name')" :error="form.error('customerName')" required>
              <template #default="{ id, invalid, describedby }">
                <UiInput :id="id" v-model="form.data.customerName" autocomplete="off" autocapitalize="words" :invalid="invalid" :describedby="describedby" />
              </template>
            </UiField>
            <UiField id="f-customerLocale" :label="$t('install.language')">
              <UiSegmented v-model="form.data.customerLocale" :options="localeOptions" :label="$t('install.language')" />
            </UiField>
            <UiField id="f-address" :label="$t('install.address')" :hint="$t('install.addressHint')" :error="form.error('address')" required>
              <template #aside>
                <UiButton size="sm" variant="ghost" :icon="PhCrosshairSimple" :loading="locating" @click="locate">
                  {{ locating ? $t('install.locating') : $t('install.useLocation') }}
                </UiButton>
              </template>
              <template #default="{ id, invalid, describedby }">
                <UiTextarea :id="id" v-model="form.data.address" :rows="2" :invalid="invalid" :describedby="describedby" />
              </template>
            </UiField>
            <p v-if="location" class="location" :class="`location--${location}`" role="status">
              <PhMapPin :size="16" weight="fill" aria-hidden="true" />
              <span>{{ location === 'saved' ? $t('install.locationSaved') : location === 'noAddress' ? $t('install.locationNoAddress') : $t('install.locationFailed') }}</span>
              <span v-if="form.data.latitude !== null && location !== 'failed'" class="mono subtle">{{ form.data.latitude?.toFixed(4) }}, {{ form.data.longitude?.toFixed(4) }}</span>
            </p>
            <p v-if="consentOnFile && lookup?.customer?.whatsappOptInAt" class="consent-on">
              <PhCheckCircle :size="18" weight="fill" aria-hidden="true" />
              {{ $t('install.consentGiven', { date: formatDate(lookup.customer.whatsappOptInAt) }) }}
            </p>
            <div v-else class="consent">
              <UiCheckbox id="f-whatsappConsent" v-model="form.data.whatsappConsent" :label="$t('install.consent')" :hint="$t('install.consentHint')" />
            </div>
          </div>
        </UiCard>

        <UiCard>
          <template #header>
            <div class="step"><span class="step__n">3</span><div><h2 class="step__title">{{ $t('install.termsStep') }}</h2></div></div>
          </template>
          <div class="stack">
            <UiField id="f-installedOn" :label="$t('install.installedOn')" :error="form.error('installedOn')" required>
              <template #default="{ id, invalid, describedby }">
                <UiInput :id="id" v-model="form.data.installedOn" type="date" :max="todayIso()" :invalid="invalid" :describedby="describedby" />
              </template>
            </UiField>
            <UiField id="f-warrantyMonths" :label="$t('install.warranty')" :error="form.error('warrantyMonths')">
              <UiSegmented v-model="warrantyChoice" :options="warrantyOptions" :label="$t('install.warranty')" block />
            </UiField>
            <UiField v-if="warrantyChoice === 'custom'" id="f-warranty-custom" :label="$t('install.warrantyMonths')">
              <UiStepper id="f-warranty-custom" v-model="form.data.warrantyMonths" :label="$t('install.warrantyMonths')" :min="1" :max="120" />
            </UiField>
            <UiField id="f-serviceIntervalMonths" :label="$t('install.interval')" :error="form.error('serviceIntervalMonths')">
              <div class="inline">
                <UiStepper id="f-serviceIntervalMonths" v-model="form.data.serviceIntervalMonths" :label="$t('install.interval')" :min="1" :max="60" />
                <span class="muted small">{{ $t('install.intervalUnit') }}</span>
              </div>
            </UiField>
            <p v-if="preview" class="preview num">{{ preview }}</p>
            <UiField id="f-notes" :label="$t('install.notes')" :hint="$t('install.notesHint')" :error="form.error('notes')" optional>
              <template #default="{ id, invalid, describedby }">
                <UiTextarea :id="id" v-model="form.data.notes" :rows="2" :invalid="invalid" :describedby="describedby" />
              </template>
            </UiField>
          </div>
        </UiCard>
      </div>

      <div class="savebar span-all">
        <UiButton type="submit" variant="accent" size="lg" block :icon="willSend ? PhPaperPlaneTilt : PhCheckCircle" :loading="form.processing.value">
          {{ willSend ? $t('install.save') : $t('install.saveOnly') }}
        </UiButton>
      </div>
    </form>
  </AppPage>
</template>

<style scoped>
.install {
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  align-items: start;
}
.install__side {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.step {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.step__n {
  display: grid;
  place-items: center;
  flex: none;
  width: 30px;
  height: 30px;
  border-radius: 10px;
  color: var(--on-primary);
  background: linear-gradient(180deg, var(--brand-500), var(--brand-700));
  font-family: var(--font-display);
  font-weight: 700;
  box-shadow: var(--primary-shadow);
}
.step__title {
  font-size: var(--text-lg);
}
.step__hint {
  margin-top: 2px;
  font-size: var(--text-sm);
  color: var(--text-muted);
}
.returning {
  padding: 12px 14px;
  border: 1px solid var(--primary-soft-border);
  border-radius: var(--radius);
  background: var(--primary-soft);
}
.returning__title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 700;
  color: var(--primary-soft-text);
}
.returning__units {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid var(--primary-soft-border);
}
.location {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 8px;
  margin-top: -6px;
  font-size: var(--text-sm);
  color: var(--success-text);
}
.location--failed,
.location--noAddress {
  color: var(--warning-text);
}
.consent {
  padding: 14px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius);
  background: var(--surface-muted);
}
.consent-on {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-radius: var(--radius);
  background: var(--success-soft);
  color: var(--success-text);
  font-size: var(--text-sm);
  font-weight: 600;
}
.inline {
  display: flex;
  align-items: center;
  gap: 12px;
}
.preview {
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  border: 1px dashed var(--primary-soft-border);
  background: var(--primary-soft);
  color: var(--primary-soft-text);
  font-size: var(--text-sm);
  font-weight: 600;
}
.savebar {
  display: flex;
  justify-content: flex-end;
}
.savebar :deep(.btn) {
  max-width: 420px;
}
@media (max-width: 899px) {
  .install {
    grid-template-columns: minmax(0, 1fr);
  }
  /* one long column on a phone: keep the save button in reach, above the tab bar */
  .savebar {
    position: sticky;
    z-index: 5;
    bottom: calc(var(--bottom-nav-h) + env(safe-area-inset-bottom) + 10px);
    padding: 10px;
    border: 1px solid var(--border);
    border-radius: var(--radius-lg);
    background: color-mix(in srgb, var(--surface) 88%, transparent);
    backdrop-filter: blur(10px);
    box-shadow: var(--shadow-lg);
  }
  .savebar :deep(.btn) {
    max-width: none;
  }
}
</style>
