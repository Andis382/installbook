<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { PhArrowSquareOut, PhBarcode, PhDeviceMobile, PhPlus, PhWhatsappLogo } from '@phosphor-icons/vue'
import AppPage from '@/components/layout/AppPage.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiCopy from '@/components/ui/UiCopy.vue'
import UiNotice from '@/components/ui/UiNotice.vue'
import UiSkeleton from '@/components/ui/UiSkeleton.vue'
import WarrantyCertificate from '@/components/units/WarrantyCertificate.vue'
import { api } from '@/lib/api'
import { formatPhone } from '@/lib/format'
import { unitName } from '@/lib/units'
import type { UnitDetail } from '@/lib/types'
import { useAuth } from '@/stores/auth'

const route = useRoute()
const auth = useAuth()
const unit = ref<UnitDetail | null>(null)

onMounted(async () => {
  unit.value = await api.get<UnitDetail>(`/units/${route.params.id}`)
})

/** Where the warranty card stands: sent, waiting in the outbox, failed, or never sent (no consent). */
const state = computed(() => {
  const m = unit.value?.lastCardMessage
  if (!m) return 'notSent'
  if (m.status === 'FAILED') return 'failed'
  if (m.status === 'SIMULATED' || m.status === 'QUEUED') return 'outbox'
  return 'sent'
})

const shareUrl = computed(() => {
  if (!unit.value) return null
  return state.value === 'outbox' && unit.value.lastCardMessage ? unit.value.lastCardMessage.waMeUrl : unit.value.cardShareUrl
})
</script>

<template>
  <AppPage
    :title="$t('installDone.title')"
    :subtitle="unit ? $t('installDone.subtitle', { unit: unitName(unit), customer: unit.customer.name }) : undefined"
  >
    <div v-if="!unit" class="grid-2">
      <UiSkeleton card :lines="4" />
      <UiSkeleton card :lines="6" />
    </div>
    <div v-else class="done">
      <div class="done__status">
        <UiCard padding="lg">
          <div class="stack">
            <UiNotice v-if="state === 'sent'" tone="success" :title="$t('installDone.sentTitle')">
              {{ $t('installDone.sentText', { phone: formatPhone(unit.customer.phone) }) }}
            </UiNotice>
            <UiNotice v-else-if="state === 'outbox'" tone="info" :title="$t('installDone.outboxTitle')">
              {{ $t('installDone.outboxText') }}
            </UiNotice>
            <UiNotice v-else-if="state === 'failed'" tone="danger" :title="$t('installDone.failedTitle')">
              {{ $t('installDone.failedText') }}
            </UiNotice>
            <UiNotice v-else tone="warning" :title="$t('installDone.notSentTitle')">
              {{ $t('installDone.notSentText', { name: unit.customer.name }) }}
            </UiNotice>

            <UiButton v-if="state !== 'sent' && shareUrl" :href="shareUrl" target="_blank" variant="primary" size="lg" block :icon="PhWhatsappLogo">
              {{ $t('installDone.sendWhatsApp') }}
            </UiButton>
            <UiCopy :value="unit.cardUrl" :label="$t('common.copy')" />
            <div class="done__actions">
              <UiButton variant="secondary" :icon="PhArrowSquareOut" :href="unit.cardUrl" target="_blank">{{ $t('installDone.openCard') }}</UiButton>
              <UiButton variant="secondary" :icon="PhBarcode" :to="{ name: 'unit', params: { id: unit.id } }">{{ $t('installDone.viewUnit') }}</UiButton>
            </div>
          </div>
        </UiCard>
        <UiButton variant="accent" size="lg" block :icon="PhPlus" :to="{ name: 'install' }">{{ $t('installDone.recordAnother') }}</UiButton>
      </div>

      <section class="done__preview" :aria-label="$t('installDone.cardPreview')">
        <WarrantyCertificate
          :business="auth.organization?.name ?? ''"
          :customer="unit.customer.name"
          :type="unit.type"
          :brand="unit.brand"
          :model="unit.model"
          :serial="unit.serialNumber"
          :installed-on="unit.installedOn"
          :warranty-until="unit.warrantyUntil"
          :warranty-active="unit.warrantyActive"
          :next-service-due="unit.nextServiceDue"
          :card-number="unit.cardNumber"
        />
        <p class="done__caption"><PhDeviceMobile :size="16" weight="bold" aria-hidden="true" /> {{ $t('installDone.cardPreview') }}</p>
      </section>
    </div>
  </AppPage>
</template>

<style scoped>
.done {
  display: grid;
  gap: 24px;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.1fr);
  align-items: start;
}
.done__status {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.done__caption {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: var(--text-sm);
  color: var(--text-muted);
}
.done__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.done__actions :deep(.btn) {
  flex: 1 1 auto;
}
.done__preview {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
@media (max-width: 899px) {
  .done {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
