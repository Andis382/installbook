<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { PhCheck, PhCrosshairSimple } from '@phosphor-icons/vue'
import AppPage from '@/components/layout/AppPage.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiField from '@/components/ui/UiField.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiTextarea from '@/components/ui/UiTextarea.vue'
import UiStepper from '@/components/ui/UiStepper.vue'
import UiNotice from '@/components/ui/UiNotice.vue'
import UiPhotoInput from '@/components/ui/UiPhotoInput.vue'
import UiSkeleton from '@/components/ui/UiSkeleton.vue'
import UiFormErrors from '@/components/ui/UiFormErrors.vue'
import TypePicker from '@/components/units/TypePicker.vue'
import { api, query } from '@/lib/api'
import { useForm } from '@/lib/form'
import { todayIso } from '@/lib/format'
import { unitName } from '@/lib/units'
import type { PlateReading, UnitDetail, UnitType } from '@/lib/types'
import { useToasts } from '@/stores/toasts'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const toasts = useToasts()

const unit = ref<UnitDetail | null>(null)
const photo = ref<File | null>(null)
const locating = ref(false)
const form = useForm({
  type: null as UnitType | null,
  brand: '',
  model: '',
  serialNumber: '',
  platePhotoId: null as string | null,
  address: '',
  latitude: null as number | null,
  longitude: null as number | null,
  installedOn: todayIso(),
  warrantyMonths: 24 as number | null,
  serviceIntervalMonths: 12 as number | null,
  notes: '',
})

onMounted(async () => {
  const u = await api.get<UnitDetail>(`/units/${route.params.id}`)
  unit.value = u
  form.reset({
    type: u.type,
    brand: u.brand,
    model: u.model ?? '',
    serialNumber: u.serialNumber ?? '',
    platePhotoId: u.platePhotoId,
    address: u.address,
    latitude: u.latitude,
    longitude: u.longitude,
    installedOn: u.installedOn,
    warrantyMonths: u.warrantyMonths,
    serviceIntervalMonths: u.serviceIntervalMonths,
    notes: u.notes ?? '',
  })
})

function locate() {
  if (!('geolocation' in navigator)) return
  locating.value = true
  navigator.geolocation.getCurrentPosition(
    async (pos) => {
      form.data.latitude = Number(pos.coords.latitude.toFixed(6))
      form.data.longitude = Number(pos.coords.longitude.toFixed(6))
      try {
        const place = await api.get<{ address: string | null }>(`/geo/reverse${query({ lat: form.data.latitude, lng: form.data.longitude })}`)
        if (place.address) form.data.address = place.address
      } finally {
        locating.value = false
      }
    },
    () => {
      locating.value = false
    },
    { enableHighAccuracy: true, timeout: 12000 },
  )
}

async function save() {
  const saved = await form.submit(async () => {
    if (photo.value) {
      const data = new FormData()
      data.append('file', photo.value)
      form.data.platePhotoId = (await api.upload<PlateReading>(`/units/plate${query({ read: false })}`, data)).photoId
    }
    return api.put<UnitDetail>(`/units/${route.params.id}`, form.data)
  })
  if (saved) {
    toasts.success(t('edit.saved'))
    await router.push({ name: 'unit', params: { id: saved.id } })
  }
}
</script>

<template>
  <AppPage
    :title="$t('edit.title')"
    :subtitle="unit ? unitName(unit) : $t('edit.subtitle')"
    :back="{ name: 'unit', params: { id: route.params.id } }"
  >
    <UiCard v-if="!unit"><UiSkeleton :lines="8" height="22px" /></UiCard>
    <form v-else class="edit" novalidate @submit.prevent="save">
      <UiFormErrors class="span-all" :errors="form.errors.value" :message="form.message.value" :trigger="form.submitted.value" />
      <UiCard>
        <div class="stack">
          <UiField id="f-type" :label="$t('install.type')" :error="form.error('type')" required>
            <TypePicker id="f-type" v-model="form.data.type" :label="$t('install.type')" :invalid="!!form.error('type')" />
          </UiField>
          <div class="grid-2">
            <UiField id="f-brand" :label="$t('install.brand')" :error="form.error('brand')" required>
              <template #default="{ id, invalid, describedby }">
                <UiInput :id="id" v-model="form.data.brand" :invalid="invalid" :describedby="describedby" />
              </template>
            </UiField>
            <UiField id="f-model" :label="$t('install.model')" :error="form.error('model')" optional>
              <template #default="{ id, invalid, describedby }">
                <UiInput :id="id" v-model="form.data.model" :invalid="invalid" :describedby="describedby" />
              </template>
            </UiField>
          </div>
          <UiField id="f-serialNumber" :label="$t('install.serial')" :hint="$t('install.serialHint')" :error="form.error('serialNumber')">
            <template #default="{ id, invalid, describedby }">
              <UiInput :id="id" v-model="form.data.serialNumber" mono size="lg" autocomplete="off" spellcheck="false" :invalid="invalid" :describedby="describedby" />
            </template>
          </UiField>
          <UiField id="f-file" :label="$t('edit.photo')" :error="form.error('file')" optional>
            <UiPhotoInput id="f-file" v-model="photo" aspect="16 / 10" :current-url="form.data.platePhotoId ? unit.platePhotoUrl : null" />
          </UiField>
        </div>
      </UiCard>

      <UiCard>
        <div class="stack">
          <UiField id="f-address" :label="$t('install.address')" :error="form.error('address')" required>
            <template #aside>
              <UiButton size="sm" variant="ghost" :icon="PhCrosshairSimple" :loading="locating" @click="locate">{{ $t('install.useLocation') }}</UiButton>
            </template>
            <template #default="{ id, invalid, describedby }">
              <UiTextarea :id="id" v-model="form.data.address" :rows="2" :invalid="invalid" :describedby="describedby" />
            </template>
          </UiField>
          <UiField id="f-installedOn" :label="$t('install.installedOn')" :error="form.error('installedOn')" required>
            <template #default="{ id, invalid, describedby }">
              <UiInput :id="id" v-model="form.data.installedOn" type="date" :max="todayIso()" :invalid="invalid" :describedby="describedby" />
            </template>
          </UiField>
          <div class="grid-2">
            <UiField id="f-warrantyMonths" :label="$t('install.warrantyMonths')" :error="form.error('warrantyMonths')">
              <UiStepper id="f-warrantyMonths" v-model="form.data.warrantyMonths" :label="$t('install.warrantyMonths')" :min="1" :max="120" />
            </UiField>
            <UiField id="f-serviceIntervalMonths" :label="`${$t('install.interval')} (${$t('install.intervalUnit')})`" :error="form.error('serviceIntervalMonths')">
              <UiStepper id="f-serviceIntervalMonths" v-model="form.data.serviceIntervalMonths" :label="$t('install.interval')" :min="1" :max="60" />
            </UiField>
          </div>
          <UiNotice tone="info">{{ $t('edit.termsHint') }}</UiNotice>
          <UiField id="f-notes" :label="$t('install.notes')" :error="form.error('notes')" optional>
            <template #default="{ id, invalid, describedby }">
              <UiTextarea :id="id" v-model="form.data.notes" :rows="3" :invalid="invalid" :describedby="describedby" />
            </template>
          </UiField>
          <div class="cluster">
            <UiButton type="submit" :icon="PhCheck" :loading="form.processing.value">{{ $t('common.save') }}</UiButton>
            <UiButton variant="ghost" :to="{ name: 'unit', params: { id: route.params.id } }">{{ $t('common.cancel') }}</UiButton>
          </div>
        </div>
      </UiCard>
    </form>
  </AppPage>
</template>

<style scoped>
.edit {
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  align-items: start;
}
@media (max-width: 899px) {
  .edit {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
