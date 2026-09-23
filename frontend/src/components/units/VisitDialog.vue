<script setup lang="ts">
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { PhCheck } from '@phosphor-icons/vue'
import UiDialog from '@/components/ui/UiDialog.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiField from '@/components/ui/UiField.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiSelect from '@/components/ui/UiSelect.vue'
import UiTextarea from '@/components/ui/UiTextarea.vue'
import UiNotice from '@/components/ui/UiNotice.vue'
import UiFormErrors from '@/components/ui/UiFormErrors.vue'
import { api } from '@/lib/api'
import { useForm } from '@/lib/form'
import { centsToInput, parseMoney, todayIso } from '@/lib/format'
import { VISIT_KINDS, type VisitKind } from '@/lib/types'
import { useToasts } from '@/stores/toasts'

/**
 * Records work done on a unit. With a booking id it closes that booking instead
 * ("mark done"), which records the same visit.
 */
const open = defineModel<boolean>('open', { default: false })
const props = withDefaults(
  defineProps<{ unitId: number; bookingId?: number | null; unitLabel: string; customerName: string; priceCents?: number | null }>(),
  { bookingId: null, priceCents: null },
)
const emit = defineEmits<{ saved: [result: unknown] }>()

const { t } = useI18n()
const toasts = useToasts()
const form = useForm({ visitedOn: todayIso(), kind: 'ANNUAL_SERVICE' as VisitKind, price: '', parts: '', notes: '' })
const kinds = computed(() => VISIT_KINDS.map((k) => ({ value: k, label: t(`visitKinds.${k}`) })))

watch(open, (value) => {
  if (value) form.reset({ visitedOn: todayIso(), price: centsToInput(props.priceCents) })
})

async function save() {
  const body = {
    visitedOn: form.data.visitedOn,
    kind: form.data.kind,
    priceCents: parseMoney(form.data.price),
    parts: form.data.parts || null,
    notes: form.data.notes || null,
  }
  const url = props.bookingId ? `/bookings/${props.bookingId}/done` : `/units/${props.unitId}/visits`
  const result = await form.submit(() => api.post(url, body))
  if (result) {
    toasts.success(props.bookingId ? t('bookings.doneToast') : t('visit.saved'))
    open.value = false
    emit('saved', result)
  }
}
</script>

<template>
  <UiDialog
    v-model:open="open"
    :title="bookingId ? $t('visit.titleDone') : $t('visit.title')"
    :description="$t('visit.description', { unit: unitLabel, customer: customerName })"
  >
    <form id="visit-form" class="stack" novalidate @submit.prevent="save">
      <UiFormErrors :errors="form.errors.value" :message="form.message.value" :trigger="form.submitted.value" />
      <div class="grid-2">
        <UiField id="f-visitedOn" :label="$t('visit.date')" :error="form.error('visitedOn')">
          <template #default="{ id, invalid, describedby }">
            <UiInput :id="id" v-model="form.data.visitedOn" type="date" :max="todayIso()" :invalid="invalid" :describedby="describedby" />
          </template>
        </UiField>
        <UiField id="f-price" :label="$t('visit.price')" :error="form.error('priceCents')" optional>
          <template #default="{ id, invalid, describedby }">
            <UiInput :id="id" v-model="form.data.price" inputmode="decimal" prefix="€" :invalid="invalid" :describedby="describedby" />
          </template>
        </UiField>
      </div>
      <UiField id="f-kind" :label="$t('visit.kind')" :error="form.error('kind')">
        <template #default="{ id }">
          <UiSelect :id="id" v-model="form.data.kind" :options="kinds" />
        </template>
      </UiField>
      <UiNotice tone="info">{{ $t('visit.cycleHint') }}</UiNotice>
      <UiField id="f-parts" :label="$t('visit.parts')" :error="form.error('parts')" optional>
        <template #default="{ id, invalid, describedby }">
          <UiInput :id="id" v-model="form.data.parts" :placeholder="$t('visit.partsPlaceholder')" :invalid="invalid" :describedby="describedby" />
        </template>
      </UiField>
      <UiField id="f-notes" :label="$t('visit.notes')" :error="form.error('notes')" optional>
        <template #default="{ id, invalid, describedby }">
          <UiTextarea :id="id" v-model="form.data.notes" :rows="2" :invalid="invalid" :describedby="describedby" />
        </template>
      </UiField>
    </form>
    <template #footer>
      <UiButton variant="ghost" @click="open = false">{{ $t('common.cancel') }}</UiButton>
      <UiButton type="submit" form="visit-form" :icon="PhCheck" :loading="form.processing.value">{{ $t('visit.save') }}</UiButton>
    </template>
  </UiDialog>
</template>
