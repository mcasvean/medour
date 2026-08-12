<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  serverError?: string | null
}>()

const emit = defineEmits<{
  submit: [payload: Record<string, unknown>]
}>()

const email = ref('')
const password = ref('')
const firstName = ref('')
const surname = ref('')
const age = ref<number | null>(null)
const gender = ref('')
const city = ref('')
const address = ref('')

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
    role: 'PATIENT',
  })
}
</script>

<template>
  <form @submit.prevent="handleSubmit">
    <p v-if="serverError" role="alert">{{ serverError }}</p>
    <label>Email <input v-model="email" type="email" required /></label>
    <label>Password <input v-model="password" type="password" required /></label>
    <label>First name <input v-model="firstName" type="text" required /></label>
    <label>Surname <input v-model="surname" type="text" required /></label>
    <label>Age <input v-model.number="age" type="number" required /></label>
    <label>Gender <input v-model="gender" type="text" required /></label>
    <label>City <input v-model="city" type="text" required /></label>
    <label>Address <input v-model="address" type="text" required /></label>
    <button type="submit">Register</button>
  </form>
</template>
