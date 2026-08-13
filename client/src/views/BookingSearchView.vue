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
        <button @click="selectDoctor(doctor.id)">Select</button>
      </li>
    </ul>

    <SlotGrid
      v-if="appointmentStore.selectedDoctorId !== null"
      :slots="appointmentStore.slots"
      @select="onSlotSelected"
    />

    <div v-if="appointmentStore.errorMessage" class="error-message">
      {{ appointmentStore.errorMessage }}
    </div>

    <div v-if="appointmentStore.bookingStep === 'confirming'" class="confirmation-panel">
      <h2>Confirm your appointment</h2>
      <p><strong>Doctor:</strong> {{ selectedDoctor?.firstName }} {{ selectedDoctor?.surname }}</p>
      <p><strong>Date:</strong> {{ appointmentStore.selectedDate }}</p>
      <p><strong>Time:</strong> {{ appointmentStore.lockedStartTime }}</p>
      <div class="confirmation-actions">
        <button @click="appointmentStore.cancelBooking()">Cancel</button>
        <button @click="appointmentStore.confirmBooking()">Confirm</button>
      </div>
    </div>

    <div v-if="appointmentStore.bookingStep === 'done'" class="booking-success">
      <p>Appointment booked successfully!</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useDoctorStore } from '../stores/doctorStore'
import { useAppointmentStore } from '../stores/appointmentStore'
import SlotGrid from '../components/SlotGrid.vue'

const doctorStore = useDoctorStore()
const appointmentStore = useAppointmentStore()
const today = new Date().toISOString().split('T')[0]

const selectedDoctor = computed(() =>
  doctorStore.doctors.find(d => d.id === appointmentStore.selectedDoctorId))

function selectDoctor(doctorId: number) {
  const dateToUse = doctorStore.filters.date || today
  appointmentStore.selectedDoctorId = doctorId
  appointmentStore.selectedDate = dateToUse
  appointmentStore.fetchSlots(doctorId, dateToUse)
  appointmentStore.connectSse(doctorId, dateToUse)
}

function onSlotSelected(startTime: string) {
  appointmentStore.lockSlot(startTime)
}

onMounted(() => {
  doctorStore.searchDoctors()
})

onUnmounted(() => {
  appointmentStore.disconnectSse()
  if (appointmentStore.bookingStep === 'confirming') {
    appointmentStore.cancelBooking()
  }
})
</script>
