<template>
  <div class="form-overlay" @click.self="$emit('cancel')">
    <div class="form-card">
      <h2>{{ mode === 'create' ? 'Add User' : 'Edit User' }}</h2>
      <form @submit.prevent="handleSubmit">
        <template v-if="mode === 'create'">
          <label>
            Email
            <input v-model="form.email" type="email" required />
          </label>
          <label>
            Password
            <input v-model="form.password" type="password" required />
          </label>
        </template>
        <label>
          First name
          <input v-model="form.firstName" type="text" required />
        </label>
        <label>
          Surname
          <input v-model="form.surname" type="text" required />
        </label>
        <label>
          Role
          <select v-model="form.role" required>
            <option value="PATIENT">PATIENT</option>
            <option value="DOCTOR">DOCTOR</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </label>
        <label>
          Age
          <input v-model.number="form.age" type="number" min="1" max="150" />
        </label>
        <label>
          Gender
          <input v-model="form.gender" type="text" />
        </label>
        <label>
          City
          <input v-model="form.city" type="text" />
        </label>
        <label>
          Address
          <input v-model="form.address" type="text" />
        </label>
        <label>
          County
          <input v-model="form.county" type="text" />
        </label>
        <label>
          Speciality
          <input v-model="form.speciality" type="text" />
        </label>
        <div v-if="errorMessage || saveError" class="form-error">{{ saveError || errorMessage }}</div>
        <div class="form-actions">
          <button type="submit" :disabled="submitting">Save</button>
          <button type="button" @click="$emit('cancel')">Cancel</button>
        </div>
      </form>
    </div>
  </div>
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
  speciality: props.user?.speciality ?? ''
})

const submitting = ref(false)
const errorMessage = ref('')

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
    speciality: form.speciality || undefined
  }
  if (props.mode === 'create') {
    payload.email = form.email
    payload.password = form.password
  }
  emit('save', payload)
}
</script>

<style scoped>
.form-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.form-card {
  background: #fff;
  border-radius: 6px;
  padding: 1.5rem;
  width: 100%;
  max-width: 480px;
  max-height: 90vh;
  overflow-y: auto;
}

.form-card h2 {
  margin: 0 0 1rem;
}

form label {
  display: flex;
  flex-direction: column;
  margin-bottom: 0.75rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: #444;
  gap: 0.25rem;
}

form input,
form select {
  border: 1px solid #ccc;
  border-radius: 4px;
  padding: 0.4rem 0.6rem;
  font-size: 0.875rem;
}

.form-error {
  color: #c53030;
  font-size: 0.85rem;
  margin-bottom: 0.5rem;
}

.form-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

.form-actions button {
  padding: 0.4rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #ccc;
}

.form-actions button[type='submit'] {
  background: #2b6cb0;
  color: #fff;
  border-color: #2b6cb0;
}

.form-actions button[type='submit']:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
