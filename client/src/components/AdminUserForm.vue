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
              <VTextField v-model="form.speciality" label="Speciality" variant="outlined" />
            </VCol>
          </template>
        </VRow>

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
import { ref, reactive } from 'vue'
import type { AdminUser } from '../stores/userStore'

const props = defineProps<{
  user?: AdminUser
  mode: 'create' | 'edit'
  saveError?: string
}>()

const emit = defineEmits<{
  (e: 'save', payload: Partial<AdminUser> & { password?: string }): void
  (e: 'cancel'): void
}>()

const form = reactive({
  email: props.user?.email ?? '',
  password: '',
  firstName: props.user?.firstName ?? '',
  surname: props.user?.surname ?? '',
  role: props.user?.role ?? 'PATIENT',
  age: props.user?.age ?? null as number | null,
  gender: props.user?.gender ?? '',
  city: props.user?.city ?? '',
  address: props.user?.address ?? '',
  county: props.user?.county ?? '',
  speciality: props.user?.speciality ?? '',
})

const submitting = ref(false)
const errorMessage = ref('')
const showPassword = ref(false)

function handleSubmit() {
  errorMessage.value = ''
  const payload: Partial<AdminUser> & { password?: string } = {
    firstName: form.firstName,
    surname: form.surname,
    role: form.role,
    age: form.age ?? undefined,
    gender: form.gender || undefined,
    city: form.city || undefined,
    address: form.address || undefined,
    county: form.county || undefined,
    speciality: form.speciality || undefined,
  }
  if (props.mode === 'create') {
    payload.email = form.email
    payload.password = form.password
  }
  emit('save', payload)
}
</script>
