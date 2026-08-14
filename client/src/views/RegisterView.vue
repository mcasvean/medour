<template>
  <VContainer fluid class="fill-height login-bg">
    <VRow justify="center" align="center" class="fill-height">
      <VCol cols="12" sm="10" md="8" lg="6">

        <VCard v-if="!selectedRole" elevation="8" rounded="xl" class="pa-2">
          <VCardText class="pa-6">
            <div class="d-flex flex-column align-center mb-6">
              <VAvatar color="primary" size="72" class="mb-4">
                <VIcon icon="mdi-account-plus" size="40" />
              </VAvatar>
              <h1 class="text-h5 font-weight-bold">Create Account</h1>
              <p class="text-body-2 text-medium-emphasis mt-1">Choose your account type to get started</p>
            </div>
            <VRow>
              <VCol cols="12" sm="6">
                <VCard
                  hover
                  rounded="xl"
                  border
                  class="text-center pa-6 cursor-pointer"
                  @click="selectedRole = 'PATIENT'"
                >
                  <VIcon icon="mdi-account-heart-outline" size="52" color="info" class="mb-3" />
                  <div class="text-h6 font-weight-bold mb-1">Patient</div>
                  <div class="text-body-2 text-medium-emphasis">Book appointments and manage your health</div>
                </VCard>
              </VCol>
              <VCol cols="12" sm="6">
                <VCard
                  hover
                  rounded="xl"
                  border
                  class="text-center pa-6 cursor-pointer"
                  @click="selectedRole = 'DOCTOR'"
                >
                  <VIcon icon="mdi-doctor" size="52" color="success" class="mb-3" />
                  <div class="text-h6 font-weight-bold mb-1">Doctor</div>
                  <div class="text-body-2 text-medium-emphasis">Manage your schedule and consult patients</div>
                </VCard>
              </VCol>
            </VRow>
            <VDivider class="my-4" />
            <p class="text-center text-body-2">
              Already have an account?
              <RouterLink to="/login" class="text-primary font-weight-medium">Sign in</RouterLink>
            </p>
          </VCardText>
        </VCard>

        <PatientRegisterForm
          v-else-if="selectedRole === 'PATIENT'"
          :server-error="serverError"
          @submit="handleSubmit"
          @back="selectedRole = null"
        />
        <DoctorRegisterForm
          v-else
          :server-error="serverError"
          @submit="handleSubmit"
          @back="selectedRole = null"
        />

      </VCol>
    </VRow>
  </VContainer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore, type User } from '../stores/authStore'
import api from '../api/index'
import PatientRegisterForm from './PatientRegisterForm.vue'
import DoctorRegisterForm from './DoctorRegisterForm.vue'

const router = useRouter()
const authStore = useAuthStore()
const selectedRole = ref<null | 'PATIENT' | 'DOCTOR'>(null)
const serverError = ref<string | null>(null)

async function handleSubmit(payload: Record<string, unknown>) {
  serverError.value = null
  try {
    const res = await api.post('/auth/register', payload)
    authStore.setAuth(res.data.token, res.data as User)
    router.push('/')
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } }).response?.status
    serverError.value = status === 409 ? 'Email already in use' : 'Registration failed. Please try again.'
  }
}
</script>

<style scoped>
.login-bg {
  background: linear-gradient(135deg, #1565C0 0%, #0277BD 100%);
}
</style>
