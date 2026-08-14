<template>
  <VContainer class="py-8" max-width="720">
    <div class="d-flex align-center mb-6">
      <VAvatar color="primary" size="56" class="mr-4">
        <VIcon icon="mdi-account" size="32" />
      </VAvatar>
      <div>
        <h1 class="text-h5 font-weight-bold">My Account</h1>
        <p class="text-body-2 text-medium-emphasis">Update your profile information</p>
      </div>
    </div>

    <VCard rounded="xl" elevation="1">
      <VCardText class="pa-6">
        <div v-if="loading" class="d-flex justify-center py-8">
          <VProgressCircular indeterminate color="primary" size="56" />
        </div>

        <VForm v-else-if="profile" @submit.prevent="handleSubmit">
          <VRow>
            <VCol cols="12" sm="6">
              <VTextField
                label="Email"
                :model-value="profile.email"
                prepend-inner-icon="mdi-email-outline"
                variant="outlined"
                readonly
                disabled
              />
            </VCol>
            <VCol cols="12" sm="6">
              <VTextField
                label="Role"
                :model-value="profile.role"
                prepend-inner-icon="mdi-shield-account-outline"
                variant="outlined"
                readonly
                disabled
              />
            </VCol>
            <VCol cols="12" sm="6">
              <VTextField v-model="profile.firstName" label="First Name" variant="outlined" required>
                <template #label>First Name <span class="text-error ml-1">*</span></template>
              </VTextField>
            </VCol>
            <VCol cols="12" sm="6">
              <VTextField v-model="profile.surname" label="Surname" variant="outlined" required>
                <template #label>Surname <span class="text-error ml-1">*</span></template>
              </VTextField>
            </VCol>
            <VCol cols="12" sm="4">
              <VTextField v-model.number="profile.age" label="Age" type="number" variant="outlined" />
            </VCol>
            <VCol cols="12" sm="4">
              <VSelect
                v-model="profile.gender"
                label="Gender"
                :items="['Male', 'Female', 'Other']"
                variant="outlined"
              />
            </VCol>
            <VCol cols="12" sm="4">
              <VTextField v-model="profile.city" label="City" variant="outlined" />
            </VCol>
            <VCol cols="12">
              <VTextField v-model="profile.address" label="Address" prepend-inner-icon="mdi-map-marker-outline" variant="outlined" />
            </VCol>
            <template v-if="profile.role === 'DOCTOR'">
              <VCol cols="12" sm="6">
                <VTextField v-model="profile.county" label="County" variant="outlined" />
              </VCol>
              <VCol cols="12" sm="6">
                <VTextField v-model="profile.speciality" label="Speciality" prepend-inner-icon="mdi-medical-bag" variant="outlined" />
              </VCol>
            </template>
          </VRow>

          <VBtn type="submit" color="primary" :loading="saving" rounded="lg">
            <VIcon start icon="mdi-content-save-outline" />Save Changes
          </VBtn>
        </VForm>
      </VCardText>
    </VCard>
  </VContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api/index'
import { useAuthStore } from '../stores/authStore'
import { useToastStore } from '../stores/toastStore'

interface ProfileData {
  id: number
  email: string
  firstName: string
  surname: string
  role: string
  age: number | null
  gender: string | null
  city: string | null
  address: string | null
  county: string | null
  speciality: string | null
}

const authStore = useAuthStore()
const toastStore = useToastStore()

const profile = ref<ProfileData | null>(null)
const loading = ref(true)
const saving = ref(false)

onMounted(async () => {
  try {
    const res = await api.get<ProfileData>('/users/me')
    profile.value = res.data
  } catch {
    toastStore.show('Failed to load profile.', 'error')
  } finally {
    loading.value = false
  }
})

async function handleSubmit() {
  if (!profile.value) return
  saving.value = true
  try {
    const res = await api.put<ProfileData>('/users/me', {
      firstName: profile.value.firstName,
      surname: profile.value.surname,
      age: profile.value.age,
      gender: profile.value.gender,
      city: profile.value.city,
      address: profile.value.address,
      county: profile.value.county,
      speciality: profile.value.speciality,
    })
    profile.value = res.data
    authStore.updateUser({ firstName: res.data.firstName, surname: res.data.surname })
    toastStore.show('Profile updated.', 'success')
  } catch (err: unknown) {
    toastStore.showError(err)
  } finally {
    saving.value = false
  }
}
</script>
