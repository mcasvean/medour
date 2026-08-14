<template>
  <VContainer class="py-8" max-width="480">
    <VCard rounded="xl" elevation="1">
      <VCardText class="pa-6">
        <div class="d-flex align-center mb-6">
          <VAvatar color="warning" size="52" class="mr-3">
            <VIcon icon="mdi-lock-reset" size="28" />
          </VAvatar>
          <div>
            <h1 class="text-h5 font-weight-bold">Change Password</h1>
            <p class="text-body-2 text-medium-emphasis">Update your account password</p>
          </div>
        </div>

        <VForm @submit.prevent="handleSubmit">
          <VTextField
            v-model="currentPassword"
            label="Current Password"
            :type="showCurrent ? 'text' : 'password'"
            prepend-inner-icon="mdi-lock-outline"
            :append-inner-icon="showCurrent ? 'mdi-eye-off' : 'mdi-eye'"
            variant="outlined"
            autocomplete="current-password"
            required
            class="mb-3"
            @click:append-inner="showCurrent = !showCurrent"
          />
          <VTextField
            v-model="newPassword"
            label="New Password"
            :type="showNew ? 'text' : 'password'"
            prepend-inner-icon="mdi-lock-plus-outline"
            :append-inner-icon="showNew ? 'mdi-eye-off' : 'mdi-eye'"
            variant="outlined"
            autocomplete="new-password"
            required
            class="mb-3"
            @click:append-inner="showNew = !showNew"
          />
          <VTextField
            v-model="confirmPassword"
            label="Confirm New Password"
            :type="showConfirm ? 'text' : 'password'"
            prepend-inner-icon="mdi-lock-check-outline"
            :append-inner-icon="showConfirm ? 'mdi-eye-off' : 'mdi-eye'"
            variant="outlined"
            autocomplete="new-password"
            required
            class="mb-4"
            @click:append-inner="showConfirm = !showConfirm"
          />

          <VAlert v-if="errorMessage" type="error" variant="tonal" class="mb-4" rounded="lg">
            {{ errorMessage }}
          </VAlert>

          <VBtn type="submit" color="primary" size="large" block :loading="saving" rounded="lg">
            Change Password
          </VBtn>
        </VForm>
      </VCardText>
    </VCard>
  </VContainer>
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
const showCurrent = ref(false)
const showNew = ref(false)
const showConfirm = ref(false)

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
      newPassword: newPassword.value,
    })
    authStore.updateUser({ mustChangePassword: false })
    router.push('/')
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } })?.response?.status
    errorMessage.value = status === 403 ? 'Current password is incorrect' : 'An error occurred. Please try again.'
  } finally {
    saving.value = false
  }
}
</script>
