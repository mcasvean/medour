<template>
  <div class="booking-search">
    <h1>Find a Doctor</h1>
    <form @submit.prevent="doctorStore.searchDoctors()">
      <div class="filters">
        <input
          v-model="doctorStore.filters.speciality"
          type="text"
          placeholder="Speciality"
          aria-label="Speciality"
        />
        <input
          v-model="doctorStore.filters.county"
          type="text"
          placeholder="County"
          aria-label="County"
        />
        <input
          v-model="doctorStore.filters.city"
          type="text"
          placeholder="City"
          aria-label="City"
        />
        <input
          v-model="doctorStore.filters.date"
          type="date"
          :min="today"
          aria-label="Date"
        />
        <button type="submit" :disabled="doctorStore.loading">Search</button>
      </div>
    </form>

    <div v-if="doctorStore.loading" class="loading">Loading...</div>

    <div v-else-if="doctorStore.doctors.length === 0" class="empty-state">
      No doctors found.
    </div>

    <ul v-else class="doctor-list">
      <li v-for="doctor in doctorStore.doctors" :key="doctor.id" class="doctor-card">
        <strong>{{ doctor.firstName }} {{ doctor.surname }}</strong>
        <span>{{ doctor.speciality }}</span>
        <span>{{ doctor.county }}, {{ doctor.city }}</span>
        <span>Rating: {{ doctor.averageRating !== null ? doctor.averageRating : 'No rating' }}</span>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useDoctorStore } from '../stores/doctorStore'

const doctorStore = useDoctorStore()
const today = new Date().toISOString().split('T')[0]

onMounted(() => {
  doctorStore.searchDoctors()
})
</script>
