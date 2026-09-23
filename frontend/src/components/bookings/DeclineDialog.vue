<script setup lang="ts">
import { watch } from 'vue'
import { useI18n } from 'vue-i18n'
import UiDialog from '@/components/ui/UiDialog.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiField from '@/components/ui/UiField.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiCheckbox from '@/components/ui/UiCheckbox.vue'
import UiFormErrors from '@/components/ui/UiFormErrors.vue'
import { api } from '@/lib/api'
import { useForm } from '@/lib/form'
import type { Booking, BookingAction } from '@/lib/types'
import { useToasts } from '@/stores/toasts'

const open = defineModel<boolean>('open', { default: false })
const props = defineProps<{ booking: Booking | null }>()
const emit = defineEmits<{ saved: [result: BookingAction] }>()

const { t } = useI18n()
const toasts = useToasts()
const form = useForm({ notifyCustomer: true, reason: '' })

watch(open, (value) => {
  if (value && props.booking) form.reset({ notifyCustomer: props.booking.whatsappOptIn })
})

async function decline() {
  if (!props.booking) return
  const result = await form.submit(() =>
    api.post<BookingAction>(`/bookings/${props.booking!.id}/decline`, {
      notifyCustomer: form.data.notifyCustomer,
      reason: form.data.reason || null,
    }),
  )
  if (result) {
    open.value = false
    toasts.success(t('bookings.declined'))
    emit('saved', result)
  }
}
</script>

<template>
  <UiDialog v-model:open="open" :title="$t('bookings.declineTitle')" :description="$t('bookings.declineText')" size="sm">
    <form id="decline-form" class="stack" novalidate @submit.prevent="decline">
      <UiFormErrors :errors="form.errors.value" :message="form.message.value" :trigger="form.submitted.value" />
      <UiCheckbox v-if="booking?.whatsappOptIn" v-model="form.data.notifyCustomer" :label="$t('bookings.declineNotify')" />
      <UiField v-if="form.data.notifyCustomer && booking?.whatsappOptIn" id="f-reason" :label="$t('bookings.reason')" :hint="$t('bookings.reasonHint')" :error="form.error('reason')" optional>
        <template #default="{ id, invalid, describedby }">
          <UiInput :id="id" v-model="form.data.reason" maxlength="300" :invalid="invalid" :describedby="describedby" />
        </template>
      </UiField>
    </form>
    <template #footer>
      <UiButton variant="ghost" @click="open = false">{{ $t('common.cancel') }}</UiButton>
      <UiButton type="submit" form="decline-form" variant="danger" :loading="form.processing.value">{{ $t('bookings.decline') }}</UiButton>
    </template>
  </UiDialog>
</template>
