<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  PhArrowClockwise,
  PhChatCircleDots,
  PhPaperPlaneTilt,
  PhRobot,
  PhWhatsappLogo,
} from '@phosphor-icons/vue'
import AppPage from '@/components/layout/AppPage.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiBadge from '@/components/ui/UiBadge.vue'
import UiEmpty from '@/components/ui/UiEmpty.vue'
import UiField from '@/components/ui/UiField.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiTextarea from '@/components/ui/UiTextarea.vue'
import UiTabs from '@/components/ui/UiTabs.vue'
import UiNotice from '@/components/ui/UiNotice.vue'
import UiSkeleton from '@/components/ui/UiSkeleton.vue'
import { api } from '@/lib/api'
import { useForm } from '@/lib/form'
import { formatPhone, formatStamp } from '@/lib/format'
import { useAuth } from '@/stores/auth'

export type OutboundMessage = {
  id: number
  recipient: string
  recipientName: string | null
  templateKey: string
  body: string
  status: 'QUEUED' | 'SENT' | 'DELIVERED' | 'READ' | 'FAILED' | 'SIMULATED'
  error: string | null
  createdAt: string
  waMeUrl: string
}
type InboundMessage = {
  id: number
  fromPhone: string
  body: string | null
  kind: string
  handled: boolean
  handledBy: string | null
  receivedAt: string
}

const { t } = useI18n()
const auth = useAuth()
const tab = ref<'out' | 'in'>('out')
const outbox = ref<OutboundMessage[] | null>(null)
const inbox = ref<InboundMessage[] | null>(null)
const sim = useForm({ from: '', body: '' })
const simReplies = ref<OutboundMessage[] | null>(null)

const tabs = computed(() => [
  {
    value: 'out' as const,
    label: t('messages.outbox'),
    icon: PhPaperPlaneTilt,
    count: outbox.value?.length ?? null,
  },
  {
    value: 'in' as const,
    label: t('messages.inbox'),
    icon: PhChatCircleDots,
    count: inbox.value?.length ?? null,
  },
])
const anySimulated = computed(() => outbox.value?.some((m) => m.status === 'SIMULATED'))

const tone: Record<
  OutboundMessage['status'],
  'neutral' | 'primary' | 'success' | 'warning' | 'danger' | 'info'
> = {
  QUEUED: 'neutral',
  SENT: 'info',
  DELIVERED: 'primary',
  READ: 'success',
  FAILED: 'danger',
  SIMULATED: 'warning',
}

async function load() {
  const [o, i] = await Promise.all([
    api.get<OutboundMessage[]>('/messages'),
    api.get<InboundMessage[]>('/messages/inbound'),
  ])
  outbox.value = o
  inbox.value = i
  // The simulator answers as whoever got the latest service reminder, the reply worth trying.
  const reminded = o.find(
    (m) => m.templateKey === 'service_due' || m.templateKey === 'service_overdue',
  )
  if (!sim.data.from && reminded) sim.data.from = formatPhone(reminded.recipient)
}

onMounted(load)

async function retry(m: OutboundMessage) {
  const updated = await api.post<OutboundMessage>(`/messages/${m.id}/retry`)
  Object.assign(m, updated)
}

async function simulate() {
  const res = await sim.submit(() =>
    api.post<{ replies: OutboundMessage[] }>('/dev/inbound', sim.data),
  )
  if (res) {
    simReplies.value = res.replies
    sim.data.body = ''
    await load()
  }
}
</script>

<template>
  <AppPage :title="$t('messages.title')" :subtitle="$t('messages.subtitle')">
    <UiNotice v-if="anySimulated" tone="info">{{ $t('messages.simulatedHint') }}</UiNotice>

    <div class="layout" :class="{ 'layout--demo': auth.demo }">
      <UiCard padding="none">
        <div class="tabs-wrap">
          <UiTabs v-model="tab" :tabs="tabs" :label="$t('messages.title')" />
        </div>
        <UiSkeleton v-if="!outbox" :lines="5" height="18px" class="pad" />
        <template v-else-if="tab === 'out'">
          <UiEmpty
            v-if="!outbox.length"
            :icon="PhPaperPlaneTilt"
            :title="$t('messages.empty')"
            compact
          />
          <ul v-else class="msgs">
            <li v-for="m in outbox" :key="m.id" class="msg">
              <div class="msg__head">
                <div class="msg__to">
                  <span class="strong">{{ m.recipientName || formatPhone(m.recipient) }}</span>
                  <span v-if="m.recipientName" class="small subtle">{{
                    formatPhone(m.recipient)
                  }}</span>
                </div>
                <UiBadge :tone="tone[m.status]" dot size="sm">{{
                  $t(`messages.status.${m.status}`)
                }}</UiBadge>
              </div>
              <p class="msg__body">{{ m.body }}</p>
              <p v-if="m.error" class="small" style="color: var(--danger-text)">{{ m.error }}</p>
              <div class="msg__foot">
                <span class="xsmall subtle">{{ formatStamp(m.createdAt) }}</span>
                <div class="cluster">
                  <UiButton
                    v-if="m.status === 'FAILED'"
                    size="sm"
                    variant="ghost"
                    :icon="PhArrowClockwise"
                    @click="retry(m)"
                    >{{ $t('messages.retry') }}</UiButton
                  >
                  <UiButton
                    size="sm"
                    variant="secondary"
                    :icon="PhWhatsappLogo"
                    :href="m.waMeUrl"
                    target="_blank"
                    >{{ $t('messages.openWhatsApp') }}</UiButton
                  >
                </div>
              </div>
            </li>
          </ul>
        </template>
        <template v-else>
          <UiEmpty
            v-if="!inbox?.length"
            :icon="PhChatCircleDots"
            :title="$t('messages.emptyInbox')"
            compact
          />
          <ul v-else class="msgs">
            <li v-for="m in inbox" :key="m.id" class="msg">
              <div class="msg__head">
                <span class="strong">{{ formatPhone(m.fromPhone) }}</span>
                <UiBadge :tone="m.handled ? 'success' : 'warning'" size="sm">{{
                  m.handled ? $t('messages.handledBy') : $t('messages.unhandled')
                }}</UiBadge>
              </div>
              <p class="msg__body">{{ m.body ?? `[${m.kind}]` }}</p>
              <span class="xsmall subtle">{{ formatStamp(m.receivedAt) }}</span>
            </li>
          </ul>
        </template>
      </UiCard>

      <UiCard
        v-if="auth.demo"
        :title="$t('messages.simulator')"
        :subtitle="$t('messages.simulatorHint')"
        :icon="PhRobot"
        tone="muted"
      >
        <form class="stack" novalidate @submit.prevent="simulate">
          <UiField id="f-from" :label="$t('messages.from')" :error="sim.error('from')">
            <template #default="{ id, invalid }">
              <UiInput
                :id="id"
                v-model="sim.data.from"
                type="tel"
                inputmode="tel"
                :invalid="invalid"
              />
            </template>
          </UiField>
          <UiField id="f-body" :label="$t('messages.body')" :error="sim.error('body')">
            <template #default="{ id, invalid }">
              <UiTextarea :id="id" v-model="sim.data.body" :rows="2" :invalid="invalid" />
            </template>
          </UiField>
          <UiButton type="submit" :icon="PhPaperPlaneTilt" :loading="sim.processing.value">{{
            $t('messages.simulate')
          }}</UiButton>
          <div v-if="simReplies" class="stack stack-sm">
            <p v-for="r in simReplies" :key="r.id" class="bubble">{{ r.body }}</p>
          </div>
        </form>
      </UiCard>
    </div>
  </AppPage>
</template>

<style scoped>
.layout {
  display: grid;
  gap: 20px;
  align-items: start;
}
.layout--demo {
  grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
}
@media (max-width: 900px) {
  .layout--demo {
    grid-template-columns: minmax(0, 1fr);
  }
}
.tabs-wrap {
  padding: 6px 12px 0;
}
.pad {
  padding: 20px;
}
.msgs {
  margin: 0;
  padding: 0;
  list-style: none;
}
.msg {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
}
.msg:last-child {
  border-bottom: 0;
}
.msg__head,
.msg__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.msg__to {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}
.msg__body {
  padding: 12px 14px;
  font-size: var(--text-sm);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: 4px var(--radius) var(--radius) var(--radius);
}
.bubble {
  padding: 10px 12px;
  font-size: var(--text-sm);
  white-space: pre-wrap;
  background: #dcf8c6;
  color: #1f2c1a;
  border-radius: var(--radius) 4px var(--radius) var(--radius);
  box-shadow: var(--shadow-xs);
}
</style>
