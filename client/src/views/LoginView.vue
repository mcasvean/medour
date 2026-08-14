<template>
  <VContainer fluid class="fill-height login-bg">
    <VRow justify="center" align="center" class="fill-height">
      <VCol cols="12" sm="8" md="5" lg="4">
        <VCard elevation="8" rounded="xl" class="pa-2">
          <VCardText class="pa-6">
            <div class="d-flex flex-column align-center mb-6">
              <VAvatar color="primary" size="72" class="mb-4">
                <VIcon icon="mdi-stethoscope" size="40" />
              </VAvatar>
              <h1 class="text-h5 font-weight-bold">Welcome to Medour</h1>
              <p class="text-body-2 text-medium-emphasis mt-1">Sign in to your account</p>
            </div>

            <VForm @submit.prevent="handleLogin">
              <VTextField
                v-model="email"
                label="Email"
                type="email"
                prepend-inner-icon="mdi-email-outline"
                variant="outlined"
                autocomplete="email"
                required
                class="mb-3"
              />
              <VTextField
                v-model="password"
                label="Password"
                :type="showPassword ? 'text' : 'password'"
                prepend-inner-icon="mdi-lock-outline"
                :append-inner-icon="showPassword ? 'mdi-eye-off' : 'mdi-eye'"
                variant="outlined"
                autocomplete="current-password"
                required
                class="mb-4"
                @click:append-inner="showPassword = !showPassword"
              />

              <VAlert v-if="errorMessage" type="error" variant="tonal" class="mb-4" rounded="lg">
                {{ errorMessage }}
              </VAlert>

              <VBtn
                type="submit"
                color="primary"
                size="large"
                block
                :loading="loading"
                rounded="lg"
              >
                Sign In
              </VBtn>
            </VForm>

            <VDivider class="my-4" />
            <p class="text-center text-body-2">
              Don't have an account?
              <RouterLink to="/register" class="text-primary font-weight-medium">Register</RouterLink>
            </p>
          </VCardText>
        </VCard>
      </VCol>
    </VRow>
  </VContainer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api/index'
import { useAuthStore } from '../stores/authStore'
import type { User } from '../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const errorMessage = ref('')
const loading = ref(false)

async function handleLogin() {
  errorMessage.value = ''
  loading.value = true
  try {
    const res = await api.post('/auth/login', { email: email.value, password: password.value })
    const data = res.data
    const user: User = {
      id: data.id,
      email: data.email,
      firstName: data.firstName,
      surname: data.surname,
      role: data.role,
      mustChangePassword: data.mustChangePassword,
      profilePicture: data.profilePicture ?? null,
    }
    authStore.setAuth(data.token, user)
    router.push('/')
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } }).response?.status
    errorMessage.value = status === 401 ? 'Invalid credentials' : 'An error occurred. Please try again.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-bg {
  background: linear-gradient(135deg, #1565C0 0%, #0277BD 100%);
}
</style>
