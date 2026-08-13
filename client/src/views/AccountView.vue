<template>
  <div class="account-view">
    <h1>My Account</h1>
    <div v-if="loading">Loading…</div>
    <form v-else-if="profile" @submit.prevent="handleSubmit">
      <div>
        <label for="email">Email</label>
        <input id="email" type="email" :value="profile.email" disabled />
      </div>
      <div>
        <label for="role">Role</label>
        <input id="role" type="text" :value="profile.role" disabled />
      </div>
      <div>
        <label for="firstName">First Name</label>
        <input id="firstName" v-model="profile.firstName" type="text" required />
      </div>
      <div>
        <label for="surname">Surname</label>
        <input id="surname" v-model="profile.surname" type="text" required />
      </div>
      <div>
        <label for="age">Age</label>
        <input id="age" v-model.number="profile.age" type="number" />
      </div>
      <div>
        <label for="gender">Gender</label>
        <input id="gender" v-model="profile.gender" type="text" />
      </div>
      <div>
        <label for="city">City</label>
        <input id="city" v-model="profile.city" type="text" />
      </div>
      <div>
        <label for="address">Address</label>
        <input id="address" v-model="profile.address" type="text" />
      </div>
      <template v-if="profile.role === 'DOCTOR'">
        <div>
          <label for="county">County</label>
          <input id="county" v-model="profile.county" type="text" />
        </div>
        <div>
          <label for="speciality">Speciality</label>
          <input id="speciality" v-model="profile.speciality" type="text" />
        </div>
      </template>
      <p v-if="successMessage" role="status">{{ successMessage }}</p>
      <p v-if="errorMessage" role="alert">{{ errorMessage }}</p>
      <button type="submit" :disabled="saving">
        {{ saving ? 'Saving…' : 'Save Changes' }}
      </button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api/index'
import { useAuthStore } from '../stores/authStore'

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

const profile = ref<ProfileData | null>(null)
const loading = ref(true)
const saving = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

onMounted(async () => {
  try {
    const res = await api.get<ProfileData>('/users/me')
    profile.value = res.data
  } catch {
    errorMessage.value = 'Failed to load profile.'
  } finally {
    loading.value = false
  }
})

async function handleSubmit() {
  if (!profile.value) return
  saving.value = true
  successMessage.value = ''
  errorMessage.value = ''
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
    successMessage.value = 'Profile updated successfully.'
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { error?: string } } }
    errorMessage.value = axiosErr.response?.data?.error ?? 'Failed to update profile.'
  } finally {
    saving.value = false
  }
}
</script>
