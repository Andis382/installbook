<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  PhBellRinging,
  PhBuildings,
  PhLinkSimple,
  PhTrash,
  PhUserCircle,
  PhUsersThree,
} from '@phosphor-icons/vue'
import UiStepper from '@/components/ui/UiStepper.vue'
import type { InstallerSettings } from '@/lib/types'
import { centsToInput, parseMoney, formatPhone } from '@/lib/format'
import AppPage from '@/components/layout/AppPage.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiField from '@/components/ui/UiField.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiSelect from '@/components/ui/UiSelect.vue'
import UiSegmented from '@/components/ui/UiSegmented.vue'
import UiAvatar from '@/components/ui/UiAvatar.vue'
import UiBadge from '@/components/ui/UiBadge.vue'
import UiCopy from '@/components/ui/UiCopy.vue'
import UiFormErrors from '@/components/ui/UiFormErrors.vue'
import UiIconButton from '@/components/ui/UiIconButton.vue'
import { api } from '@/lib/api'
import { useForm } from '@/lib/form'
import { formatRelative } from '@/lib/format'
import { useAuth, type Me, type Organization } from '@/stores/auth'
import { useToasts } from '@/stores/toasts'
import { useConfirm } from '@/stores/confirm'
import { setLocale, type Locale } from '@/i18n'
import { INVITE_ROLES } from '@/config'

type Member = {
  id: number
  name: string
  email: string
  role: string
  lastLoginAt: string | null
  you: boolean
}
type Pending = { id: number; name: string | null; role: string; url: string; expiresAt: string }

const { t } = useI18n()
const auth = useAuth()
const toasts = useToasts()
const confirm = useConfirm()
const isOwner = computed(() => auth.hasRole('OWNER'))

const profile = useForm({
  name: auth.user?.name ?? '',
  locale: (auth.user?.locale ?? 'sq') as Locale,
})
const org = useForm({
  name: auth.organization?.name ?? '',
  phone: auth.organization?.phone ? formatPhone(auth.organization.phone) : '',
  locale: (auth.organization?.locale ?? 'sq') as Locale,
  timezone: auth.organization?.timezone ?? 'Europe/Tirane',
  currency: auth.organization?.currency ?? 'EUR',
})
const invite = useForm({ name: '', role: INVITE_ROLES[0] ?? 'TECHNICIAN' })
const installer = useForm({
  defaultWarrantyMonths: 24 as number | null,
  defaultServiceIntervalMonths: 12 as number | null,
  reminderLeadDays: 30 as number | null,
  price: '',
  publicPhone: '',
})

async function loadInstaller() {
  const s = await api.get<InstallerSettings>('/settings/installer')
  installer.reset({
    defaultWarrantyMonths: s.defaultWarrantyMonths,
    defaultServiceIntervalMonths: s.defaultServiceIntervalMonths,
    reminderLeadDays: s.reminderLeadDays,
    price: centsToInput(s.typicalServicePriceCents),
    publicPhone: s.publicPhone ? formatPhone(s.publicPhone) : '',
  })
}

async function saveInstaller() {
  const saved = await installer.submit(() =>
    api.put<InstallerSettings>('/settings/installer', {
      defaultWarrantyMonths: installer.data.defaultWarrantyMonths,
      defaultServiceIntervalMonths: installer.data.defaultServiceIntervalMonths,
      reminderLeadDays: installer.data.reminderLeadDays,
      typicalServicePriceCents: parseMoney(installer.data.price),
      publicPhone: installer.data.publicPhone || null,
    }),
  )
  if (saved) toasts.success(t('settings.saved'))
}

const members = ref<Member[]>([])
const pending = ref<Pending[]>([])
const lastLink = ref<string | null>(null)

const localeOptions = [
  { value: 'sq' as Locale, label: 'Shqip' },
  { value: 'en' as Locale, label: 'English' },
]
const zones = [
  'Europe/Tirane',
  'Europe/Belgrade',
  'Europe/Rome',
  'Europe/Athens',
  'Europe/Berlin',
  'Europe/London',
  'Europe/Istanbul',
].map((z) => ({
  value: z,
  label: z.replace('_', ' '),
}))
const currencies = ['EUR', 'ALL', 'GBP', 'CHF', 'USD'].map((c) => ({ value: c, label: c }))
const roleOptions = computed(() => INVITE_ROLES.map((r) => ({ value: r, label: t(`roles.${r}`) })))

async function loadTeam() {
  const team = await api.get<{ members: Member[]; invitations: Pending[] }>('/team')
  members.value = team.members
  pending.value = team.invitations
}

onMounted(() => Promise.all([loadTeam(), loadInstaller()]))

async function saveProfile() {
  const me = await profile.submit(() => api.put<Me>('/auth/me', profile.data))
  if (me) {
    auth.apply(me)
    setLocale(profile.data.locale)
    toasts.success(t('settings.saved'))
  }
}

async function saveOrg() {
  const saved = await org.submit(() => api.put<Organization>('/organization', org.data))
  if (saved && auth.organization) {
    auth.organization = saved
    toasts.success(t('settings.saved'))
  }
}

async function createInvite() {
  const created = await invite.submit(() => api.post<Pending>('/team/invitations', invite.data))
  if (created) {
    lastLink.value = created.url
    invite.reset()
    toasts.success(t('settings.linkCreated'))
    await loadTeam()
  }
}

async function revoke(p: Pending) {
  await api.delete(`/team/invitations/${p.id}`)
  if (lastLink.value === p.url) lastLink.value = null
  await loadTeam()
}

async function removeMember(m: Member) {
  const ok = await confirm.ask({
    title: t('settings.removeMember', { name: m.name }),
    text: t('settings.removeMemberText'),
    danger: true,
    confirmLabel: t('common.remove'),
  })
  if (!ok) return
  await api.delete(`/team/members/${m.id}`)
  await loadTeam()
}
</script>

<template>
  <AppPage :title="$t('settings.title')" :subtitle="$t('settings.subtitle')">
    <div class="grid-2">
      <UiCard :title="$t('settings.profile')" :icon="PhUserCircle">
        <form class="stack" novalidate @submit.prevent="saveProfile">
          <UiFormErrors :errors="profile.errors.value" :trigger="profile.submitted.value" />
          <UiField id="f-name" :label="$t('common.name')" :error="profile.error('name')">
            <template #default="{ id, invalid, describedby }">
              <UiInput
                :id="id"
                v-model="profile.data.name"
                :invalid="invalid"
                :describedby="describedby"
                autocomplete="name"
              />
            </template>
          </UiField>
          <UiField id="f-locale" :label="$t('common.language')">
            <UiSegmented
              v-model="profile.data.locale"
              :options="localeOptions"
              :label="$t('common.language')"
            />
          </UiField>
          <div>
            <UiButton type="submit" :loading="profile.processing.value">{{
              $t('common.save')
            }}</UiButton>
          </div>
        </form>
      </UiCard>

      <UiCard :title="$t('settings.organization')" :icon="PhBuildings">
        <form class="stack" novalidate @submit.prevent="saveOrg">
          <UiFormErrors :errors="org.errors.value" :trigger="org.submitted.value" />
          <UiField id="f-org-name" :label="$t('auth.organizationName')" :error="org.error('name')">
            <template #default="{ id, invalid, describedby }">
              <UiInput
                :id="id"
                v-model="org.data.name"
                :invalid="invalid"
                :describedby="describedby"
                :disabled="!isOwner"
              />
            </template>
          </UiField>
          <UiField
            id="f-org-phone"
            :label="$t('settings.phone')"
            :hint="$t('settings.phoneHint')"
            :error="org.error('phone')"
          >
            <template #default="{ id, invalid, describedby }">
              <UiInput
                :id="id"
                v-model="org.data.phone"
                type="tel"
                inputmode="tel"
                :invalid="invalid"
                :describedby="describedby"
                :disabled="!isOwner"
              />
            </template>
          </UiField>
          <UiField
            id="f-org-locale"
            :label="$t('common.language')"
            :hint="$t('settings.languageHint')"
          >
            <UiSegmented
              v-model="org.data.locale"
              :options="localeOptions"
              :label="$t('common.language')"
            />
          </UiField>
          <div class="grid-2">
            <UiField id="f-org-tz" :label="$t('settings.timezone')">
              <template #default="{ id }">
                <UiSelect
                  :id="id"
                  v-model="org.data.timezone"
                  :options="zones"
                  :disabled="!isOwner"
                />
              </template>
            </UiField>
            <UiField id="f-org-cur" :label="$t('settings.currency')">
              <template #default="{ id }">
                <UiSelect
                  :id="id"
                  v-model="org.data.currency"
                  :options="currencies"
                  :disabled="!isOwner"
                />
              </template>
            </UiField>
          </div>
          <div v-if="isOwner">
            <UiButton type="submit" :loading="org.processing.value">{{
              $t('common.save')
            }}</UiButton>
          </div>
        </form>
      </UiCard>

      <UiCard
        class="span-all"
        :title="$t('settings.installer')"
        :subtitle="$t('settings.installerHint')"
        :icon="PhBellRinging"
      >
        <form class="installer" novalidate @submit.prevent="saveInstaller">
          <UiFormErrors
            class="span-all"
            :errors="installer.errors.value"
            :message="installer.message.value"
            :trigger="installer.submitted.value"
          />
          <UiField
            id="f-defaultWarrantyMonths"
            :label="`${$t('settings.defaultWarranty')} (${$t('settings.monthsUnit')})`"
            :error="installer.error('defaultWarrantyMonths')"
          >
            <UiStepper
              id="f-defaultWarrantyMonths"
              v-model="installer.data.defaultWarrantyMonths"
              :label="$t('settings.defaultWarranty')"
              :min="1"
              :max="120"
              :step="6"
            />
          </UiField>
          <UiField
            id="f-defaultServiceIntervalMonths"
            :label="`${$t('settings.defaultInterval')} (${$t('settings.monthsUnit')})`"
            :error="installer.error('defaultServiceIntervalMonths')"
          >
            <UiStepper
              id="f-defaultServiceIntervalMonths"
              v-model="installer.data.defaultServiceIntervalMonths"
              :label="$t('settings.defaultInterval')"
              :min="1"
              :max="60"
            />
          </UiField>
          <UiField
            id="f-reminderLeadDays"
            :label="`${$t('settings.leadDays')} (${$t('settings.leadDaysUnit')})`"
            :hint="$t('settings.leadDaysHint')"
            :error="installer.error('reminderLeadDays')"
          >
            <UiStepper
              id="f-reminderLeadDays"
              v-model="installer.data.reminderLeadDays"
              :label="$t('settings.leadDays')"
              :min="0"
              :max="90"
              :step="5"
            />
          </UiField>
          <UiField
            id="f-typicalServicePriceCents"
            :label="$t('settings.price')"
            :hint="$t('settings.priceHint')"
            :error="installer.error('typicalServicePriceCents')"
          >
            <template #default="{ id, invalid, describedby }">
              <UiInput
                :id="id"
                v-model="installer.data.price"
                inputmode="decimal"
                prefix="€"
                :invalid="invalid"
                :describedby="describedby"
                :disabled="!isOwner"
              />
            </template>
          </UiField>
          <UiField
            id="f-publicPhone"
            :label="$t('settings.publicPhone')"
            :hint="$t('settings.publicPhoneHint')"
            :error="installer.error('publicPhone')"
            optional
          >
            <template #default="{ id, invalid, describedby }">
              <UiInput
                :id="id"
                v-model="installer.data.publicPhone"
                type="tel"
                inputmode="tel"
                :invalid="invalid"
                :describedby="describedby"
                :disabled="!isOwner"
              />
            </template>
          </UiField>
          <div v-if="isOwner" class="installer__save">
            <UiButton type="submit" :loading="installer.processing.value">{{
              $t('common.save')
            }}</UiButton>
          </div>
        </form>
      </UiCard>

      <UiCard
        class="span-all"
        :title="$t('settings.team')"
        :subtitle="$t('settings.teamHint')"
        :icon="PhUsersThree"
      >
        <div class="team">
          <ul class="people">
            <li v-for="m in members" :key="m.id" class="person">
              <UiAvatar :name="m.name" :size="40" />
              <div class="person__who">
                <p class="strong">
                  {{ m.name }}
                  <span v-if="m.you" class="subtle small">({{ $t('settings.you') }})</span>
                </p>
                <p class="small muted truncate">{{ m.email }}</p>
                <p class="xsmall subtle">
                  {{
                    m.lastLoginAt
                      ? $t('settings.lastSeen', { when: formatRelative(m.lastLoginAt) })
                      : $t('settings.neverSeen')
                  }}
                </p>
              </div>
              <UiBadge :tone="m.role === 'OWNER' ? 'primary' : 'neutral'">{{
                $t(`roles.${m.role}`)
              }}</UiBadge>
              <UiIconButton
                v-if="isOwner && !m.you"
                :icon="PhTrash"
                :label="$t('common.remove')"
                size="sm"
                @click="removeMember(m)"
              />
            </li>
          </ul>

          <div v-if="pending.length" class="stack stack-sm">
            <p class="eyebrow">{{ $t('settings.pending') }}</p>
            <div v-for="p in pending" :key="p.id" class="pending">
              <div class="pending__head">
                <span class="strong">{{ p.name || $t(`roles.${p.role}`) }}</span>
                <UiBadge size="sm">{{ $t(`roles.${p.role}`) }}</UiBadge>
                <UiButton v-if="isOwner" variant="ghost" size="sm" @click="revoke(p)">{{
                  $t('settings.revoke')
                }}</UiButton>
              </div>
              <UiCopy :value="p.url" />
            </div>
          </div>

          <form v-if="isOwner" class="invite" novalidate @submit.prevent="createInvite">
            <UiField id="f-invite-name" :label="$t('settings.inviteName')" optional>
              <template #default="{ id }">
                <UiInput :id="id" v-model="invite.data.name" />
              </template>
            </UiField>
            <UiField
              v-if="roleOptions.length > 1"
              id="f-invite-role"
              :label="$t('settings.inviteRole')"
            >
              <template #default="{ id }">
                <UiSelect :id="id" v-model="invite.data.role" :options="roleOptions" />
              </template>
            </UiField>
            <UiButton
              type="submit"
              variant="secondary"
              :icon="PhLinkSimple"
              :loading="invite.processing.value"
              >{{ $t('settings.invite') }}</UiButton
            >
          </form>
        </div>
      </UiCard>
    </div>
  </AppPage>
</template>

<style scoped>
.installer {
  display: grid;
  gap: 16px 20px;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 250px), 1fr));
  align-items: start;
}
.installer__save {
  grid-column: 1 / -1;
}
.team {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.people {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 320px), 1fr));
  margin: 0;
  padding: 0;
  list-style: none;
}
.person {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: var(--radius);
}
.person__who {
  flex: 1;
  min-width: 0;
}
.pending {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius);
}
.pending__head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.pending__head .btn {
  margin-left: auto;
}
.invite {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}
.invite > .field {
  flex: 1 1 200px;
}
</style>
