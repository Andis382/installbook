<script setup lang="ts">
import { watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { PhCalendarCheck, PhWhatsappLogo } from '@phosphor-icons/vue'
import UiDialog from '@/components/ui/UiDialog.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiField from '@/components/ui/UiField.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiCheckbox from '@/components/ui/UiCheckbox.vue'
import UiNotice from '@/components/ui/UiNotice.vue'
import UiFormErrors from '@/components/ui/UiFormErrors.vue'
import { api } from '@/lib/api'
import { useForm } from '@/lib/form'
import { addDays } from '@/lib/dates'
import { todayIso } from '@/lib/format'
import type { Booking, BookingAction } from '@/lib/types'
import { useToasts } from '@/stores/toasts'

const open = defineModel<boolean>('open', { default: false })
const props = defineProps<{ booking: Booking | null }>()
const emit = defineEmits<{ saved: [result: BookingAction] }>()

const { t } = useI18n()
const toasts = useToasts()
const form = useForm({ date: addDays(todayIso(), 1), time: '09:00', notifyCustomer: true })

watch(open, (value) => {
  if (!value || !props.booking) return
  const b = props.booking
  const preferred = b.preferredDate && b.preferredDate > todayIso() ? b.preferredDate : addDays(todayIso(), 1)
  form.reset({
    date: preferred,
    time: b.preferredPeriod === 'AFTERNOON' ? '14:00' : '09:00',
    notifyCustomer: b.whatsappOptIn,
  })
})

async function save() {
  if (!props.booking) return
  const result = await form.submit(() => api.post<BookingAction>(`/bookings/${props.booking!.id}/schedule`, form.data))
  if (!result) return
  open.value = false
  toasts.success(t('bookings.scheduled'))
  emit('saved', result)
}
</script>

<template>
  <UiDialog v-model:open="open" :title="$t('bookings.scheduleTitle')" :description="booking ? `${booking.customerName} · ${booking.address}` : undefined" size="sm">
    <form id="schedule-form" class="stack" novalidate @submit.prevent="save">
      <UiFormErrors :errors="form.errors.value" :message="form.message.value" :trigger="form.submitted.value" />
      <div class="grid-2">
        <UiField id="f-day" :label="$t('bookings.day')" :error="form.error('date')">
          <template #default="{ id, invalid, describedby }">
            <UiInput :id="id" v-model="form.data.date" type="date" :min="todayIso()" :invalid="invalid" :describedby="describedby" />
          </template>
        </UiField>
        <UiField id="f-time" :label="$t('bookings.time')" :error="form.error('time')">
          <template #default="{ id, invalid, describedby }">
            <UiInput :id="id" v-model="form.data.time" type="time" step="900" :invalid="invalid" :describedby="describedby" />
          </template>
        </UiField>
      </div>
      <UiCheckbox v-if="booking?.whatsappOptIn" v-model="form.data.notifyCustomer" :label="$t('bookings.notify', { name: booking.customerName })" />
      <UiNotice v-else-if="booking" tone="warning" :icon="PhWhatsappLogo">{{ $t('bookings.notifyNoConsent', { name: booking.customerName }) }}</UiNotice>
    </form>
    <template #footer>
      <UiButton variant="ghost" @click="open = false">{{ $t('common.cancel') }}</UiButton>
      <UiButton type="submit" form="schedule-form" :icon="PhCalendarCheck" :loading="form.processing.value">{{ $t('bookings.schedule') }}</UiButton>
    </template>
  </UiDialog>
</template>
