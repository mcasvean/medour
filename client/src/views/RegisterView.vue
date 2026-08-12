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
    if (status === 409) {
      serverError.value = 'Email already in use'
    } else {
      serverError.value = 'Registration failed. Please try again.'
    }
  }
}
</script>

<template>
  <div>
    <div v-if="!selectedRole">
      <button @click="selectedRole = 'PATIENT'">Register as Patient</button>
      <button @click="selectedRole = 'DOCTOR'">Register as Doctor</button>
    </div>
    <PatientRegisterForm
      v-else-if="selectedRole === 'PATIENT'"
      :server-error="serverError"
      @submit="handleSubmit"
    />
    <DoctorRegisterForm
      v-else
      :server-error="serverError"
      @submit="handleSubmit"
    />
  </div>
</template>
