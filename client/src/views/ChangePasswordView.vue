<template>
  <div class="change-password-view">
    <h1>Change Password</h1>
    <form @submit.prevent="handleSubmit">
      <div v-if="errorMessage" class="error">{{ errorMessage }}</div>
      <label>
        Current Password
        <input v-model="currentPassword" type="password" required autocomplete="current-password" />
      </label>
      <label>
        New Password
        <input v-model="newPassword" type="password" required autocomplete="new-password" />
      </label>
      <label>
        Confirm New Password
        <input v-model="confirmPassword" type="password" required autocomplete="new-password" />
      </label>
      <button type="submit" :disabled="saving">
        {{ saving ? 'Saving…' : 'Change Password' }}
      </button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const errorMessage = ref('')
const saving = ref(false)

async function handleSubmit() {
  errorMessage.value = ''

  if (newPassword.value !== confirmPassword.value) {
    errorMessage.value = 'New passwords do not match'
    return
  }

  saving.value = true
  try {
    await api.post('/users/me/password', {
      currentPassword: currentPassword.value,
      newPassword: newPassword.value
    })
    authStore.updateUser({ mustChangePassword: false })
    router.push('/')
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } })?.response?.status
    if (status === 403) {
      errorMessage.value = 'Current password is incorrect'
    } else {
      errorMessage.value = 'An error occurred. Please try again.'
    }
  } finally {
    saving.value = false
  }
}
</script>
