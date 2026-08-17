<template>
  <VCard rounded="xl">
    <VCardTitle class="pa-5 pb-2 text-h6">
      {{ mode === 'create' ? 'Add User' : 'Edit User' }}
    </VCardTitle>
    <VDivider />
    <VCardText class="pa-5">
      <VForm ref="formRef" @submit.prevent="handleSubmit">
        <VRow>
          <template v-if="mode === 'create'">
            <VCol cols="12">
              <VTextField
                v-model="form.email"
                label="Email"
                type="email"
                variant="outlined"
                required
                prepend-inner-icon="mdi-email-outline"
              >
                <template #label>Email <span class="text-error ml-1">*</span></template>
              </VTextField>
            </VCol>
            <VCol cols="12">
              <VTextField
                v-model="form.password"
                label="Password"
                :type="showPassword ? 'text' : 'password'"
                :append-inner-icon="showPassword ? 'mdi-eye-off' : 'mdi-eye'"
                variant="outlined"
                required
                prepend-inner-icon="mdi-lock-outline"
                @click:append-inner="showPassword = !showPassword"
              >
                <template #label>Password <span class="text-error ml-1">*</span></template>
              </VTextField>
            </VCol>
          </template>
          <VCol cols="12" sm="6">
            <VTextField v-model="form.firstName" label="First Name" variant="outlined" required>
              <template #label>First Name <span class="text-error ml-1">*</span></template>
            </VTextField>
          </VCol>
          <VCol cols="12" sm="6">
            <VTextField v-model="form.surname" label="Surname" variant="outlined" required>
              <template #label>Surname <span class="text-error ml-1">*</span></template>
            </VTextField>
          </VCol>
          <VCol cols="12" sm="6">
            <VSelect
              v-model="form.role"
              label="Role"
              :items="['PATIENT', 'DOCTOR', 'ADMIN']"
              variant="outlined"
              required
            >
              <template #label>Role <span class="text-error ml-1">*</span></template>
            </VSelect>
          </VCol>
          <VCol cols="12" sm="6">
            <VTextField v-model.number="form.age" label="Age" type="number" min="1" max="150" variant="outlined" />
          </VCol>
          <VCol cols="12" sm="6">
            <VSelect
              v-model="form.gender"
              label="Gender"
              :items="['Male', 'Female', 'Other']"
              variant="outlined"
            />
          </VCol>
          <VCol cols="12" sm="6">
            <VTextField v-model="form.city" label="City" variant="outlined" />
          </VCol>
          <VCol cols="12">
            <VTextField
              v-model="form.address"
              label="Address"
              variant="outlined"
              prepend-inner-icon="mdi-map-marker-outline"
            />
          </VCol>
          <template v-if="form.role === 'DOCTOR'">
            <VCol cols="12" sm="6">
              <VTextField v-model="form.county" label="County" variant="outlined" />
            </VCol>
            <VCol cols="12" sm="6">
              <VSelect
                v-model="form.specialityId"
                label="Speciality"
                :items="specialityStore.specialities"
                item-title="name"
                item-value="id"
                variant="outlined"
                clearable
              />
            </VCol>
          </template>
        </VRow>

        <template v-if="mode === 'edit'">
          <VDivider class="my-4" />
          <VAlert type="warning" variant="tonal" class="mb-4" rounded="lg">
            Reset Password — This will immediately replace the user's current password. Leave blank to keep the current password.
          </VAlert>
          <VTextField
            v-model="form.newPassword"
            label="New Password"
            :type="showNewPassword ? 'text' : 'password'"
            :append-inner-icon="showNewPassword ? 'mdi-eye-off' : 'mdi-eye'"
            variant="outlined"
            prepend-inner-icon="mdi-lock-reset"
            autocomplete="new-password"
            @click:append-inner="showNewPassword = !showNewPassword"
          />
        </template>

        <VAlert v-if="errorMessage || saveError" type="error" variant="tonal" class="mt-2" rounded="lg">
          {{ saveError || errorMessage }}
        </VAlert>
      </VForm>
    </VCardText>
    <VCardActions class="pa-5 pt-0">
      <VSpacer />
      <VBtn variant="text" @click="$emit('cancel')">Cancel</VBtn>
      <VBtn color="primary" variant="elevated" rounded="lg" :loading="submitting" @click="handleSubmit">
        Save
      </VBtn>
    </VCardActions>
  </VCard>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { AdminUser } from '../stores/userStore'
import { useSpecialityStore } from '../stores/specialityStore'

const props = defineProps<{
  user?: AdminUser
  mode: 'create' | 'edit'
  saveError?: string
}>()

const emit = defineEmits<{
  (e: 'save', payload: Partial<AdminUser> & { password?: string; newPassword?: string }): void
  (e: 'cancel'): void
}>()

const specialityStore = useSpecialityStore()

onMounted(async () => {
  if (specialityStore.specialities.length === 0) {
    await specialityStore.fetchSpecialities()
  }
})

const form = reactive({
  email: props.user?.email ?? '',
  password: '',
  newPassword: '',
  firstName: props.user?.firstName ?? '',
  surname: props.user?.surname ?? '',
  role: props.user?.role ?? 'PATIENT',
  age: props.user?.age ?? null as number | null,
  gender: props.user?.gender ?? '',
  city: props.user?.city ?? '',
  address: props.user?.address ?? '',
  county: props.user?.county ?? '',
  specialityId: props.user?.specialityId ?? null as number | null,
})

const submitting = ref(false)
const errorMessage = ref('')
const showPassword = ref(false)
const showNewPassword = ref(false)

function handleSubmit() {
  errorMessage.value = ''
  const payload: Partial<AdminUser> & { password?: string; newPassword?: string } = {
    firstName: form.firstName,
    surname: form.surname,
    role: form.role,
    age: form.age ?? undefined,
    gender: form.gender || undefined,
    city: form.city || undefined,
    address: form.address || undefined,
    county: form.county || undefined,
    specialityId: form.specialityId ?? null,
  }
  if (props.mode === 'create') {
    payload.email = form.email
    payload.password = form.password
  }
  if (form.newPassword?.trim()) {
    payload.newPassword = form.newPassword.trim()
  }
  emit('save', payload)
}
</script>
