<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  serverError?: string | null
}>()

const emit = defineEmits<{
  submit: [payload: Record<string, unknown>]
  back: []
}>()

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const firstName = ref('')
const surname = ref('')
const age = ref<number | null>(null)
const gender = ref('')
const city = ref('')
const address = ref('')
const county = ref('')
const speciality = ref('')

function handleSubmit() {
  emit('submit', {
    email: email.value,
    password: password.value,
    firstName: firstName.value,
    surname: surname.value,
    age: age.value,
    gender: gender.value,
    city: city.value,
    address: address.value,
    county: county.value,
    speciality: speciality.value,
    role: 'DOCTOR',
  })
}
</script>

<template>
  <VCard elevation="8" rounded="xl" class="pa-2">
    <VCardText class="pa-6">
      <div class="d-flex align-center mb-5">
        <VBtn icon="mdi-arrow-left" variant="text" @click="emit('back')" />
        <div class="ml-2">
          <h1 class="text-h5 font-weight-bold">Doctor Registration</h1>
          <p class="text-body-2 text-medium-emphasis">Fill in your professional details</p>
        </div>
      </div>

      <VAlert v-if="serverError" type="error" variant="tonal" class="mb-4" rounded="lg">
        {{ serverError }}
      </VAlert>

      <VForm @submit.prevent="handleSubmit">
        <VRow>
          <VCol cols="12" sm="6">
            <VTextField v-model="firstName" label="First Name" prepend-inner-icon="mdi-account-outline" variant="outlined" required />
          </VCol>
          <VCol cols="12" sm="6">
            <VTextField v-model="surname" label="Surname" variant="outlined" required />
          </VCol>
          <VCol cols="12">
            <VTextField v-model="email" label="Email" type="email" prepend-inner-icon="mdi-email-outline" variant="outlined" autocomplete="email" required />
          </VCol>
          <VCol cols="12">
            <VTextField
              v-model="password"
              label="Password"
              :type="showPassword ? 'text' : 'password'"
              prepend-inner-icon="mdi-lock-outline"
              :append-inner-icon="showPassword ? 'mdi-eye-off' : 'mdi-eye'"
              variant="outlined"
              autocomplete="new-password"
              required
              @click:append-inner="showPassword = !showPassword"
            />
          </VCol>
          <VCol cols="12" sm="6">
            <VTextField v-model.number="age" label="Age" type="number" variant="outlined" required />
          </VCol>
          <VCol cols="12" sm="6">
            <VSelect v-model="gender" label="Gender" :items="['Male', 'Female', 'Other']" variant="outlined" required />
          </VCol>
          <VCol cols="12">
            <VTextField v-model="speciality" label="Speciality" prepend-inner-icon="mdi-medical-bag" variant="outlined" required />
          </VCol>
          <VCol cols="12" sm="4">
            <VTextField v-model="county" label="County" variant="outlined" required />
          </VCol>
          <VCol cols="12" sm="4">
            <VTextField v-model="city" label="City" prepend-inner-icon="mdi-city-outline" variant="outlined" required />
          </VCol>
          <VCol cols="12" sm="4">
            <VTextField v-model="address" label="Address" prepend-inner-icon="mdi-map-marker-outline" variant="outlined" required />
          </VCol>
        </VRow>
        <VBtn type="submit" color="primary" size="large" block rounded="lg" class="mt-2">
          Create Account
        </VBtn>
      </VForm>
    </VCardText>
  </VCard>
</template>
