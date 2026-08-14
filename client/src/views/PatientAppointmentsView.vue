<template>
  <div class="patient-appointments">
    <h1>My Appointments</h1>

    <div v-if="loading" class="loading">Loading...</div>

    <div v-else-if="appointmentStore.patientAppointments.length === 0" class="empty-state">
      You have no appointments yet.
    </div>

    <ul v-else class="appointment-list">
      <li
        v-for="appt in appointmentStore.patientAppointments"
        :key="appt.id"
        class="appointment-card"
      >
        <div class="card-header">
          <span class="doctor-name">
            {{ appt.doctorFirstName }} {{ appt.doctorSurname }}
            <span v-if="appt.doctorRemoved" class="badge badge-removed">Removed</span>
          </span>
          <span
            class="badge"
            :class="{
              'badge-open': appt.status === 'OPEN',
              'badge-completed': appt.status === 'COMPLETED',
              'badge-canceled': appt.status === 'CANCELED',
              'badge-auto-canceled': appt.status === 'AUTO_CANCELED',
            }"
          >{{ appt.status }}</span>
        </div>
        <div class="card-body">
          <span>{{ appt.doctorSpeciality }}</span>
          <span>{{ appt.scheduledDate }} at {{ appt.startTime }}</span>
        </div>
        <button
          v-if="appt.status === 'OPEN' && appt.wherebyRoomUrl"
          :disabled="!isJoinActive(appt.scheduledDate, appt.startTime, appt.status)"
          @click="join(appt.wherebyRoomUrl!)"
        >Join</button>
        <div v-if="appt.status === 'COMPLETED'" class="rating-widget">
          <label :for="'rating-' + appt.id">Rate (1–10):</label>
          <input
            :id="'rating-' + appt.id"
            v-model.number="ratingInputs[appt.id]"
            type="number"
            min="1"
            max="10"
            :disabled="appt.ratingId !== null || submittingRating[appt.id]"
          />
          <button
            :disabled="appt.ratingId !== null || submittingRating[appt.id]"
            @click="saveRating(appt.id)"
          >Save</button>
          <span v-if="ratingErrors[appt.id]" class="rating-error">{{ ratingErrors[appt.id] }}</span>
        </div>
        <div class="card-footer">
          <small class="created-at">Booked: {{ appt.createdAt }}</small>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useAppointmentStore, isJoinActive } from '../stores/appointmentStore'

const appointmentStore = useAppointmentStore()
const loading = ref(false)
const ratingInputs = ref<Record<number, number | null>>({})
const submittingRating = ref<Record<number, boolean>>({})
const ratingErrors = ref<Record<number, string>>({})

function join(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer')
}

async function saveRating(appointmentId: number) {
  const value = ratingInputs.value[appointmentId]
  if (value == null || Number.isNaN(value) || value < 1 || value > 10) return
  if (submittingRating.value[appointmentId]) return
  submittingRating.value[appointmentId] = true
  ratingErrors.value[appointmentId] = ''
  try {
    await appointmentStore.submitRating(appointmentId, value)
  } catch {
    ratingErrors.value[appointmentId] = 'Could not save rating. Please try again.'
  } finally {
    submittingRating.value[appointmentId] = false
  }
}

// Pre-fill inputs from existing rating values; do not overwrite values the user has already typed
watch(
  () => appointmentStore.patientAppointments,
  appointments => {
    for (const appt of appointments) {
      if (appt.status === 'COMPLETED' && !(appt.id in ratingInputs.value)) {
        ratingInputs.value[appt.id] = appt.ratingValue
      }
    }
  },
  { immediate: true },
)

onMounted(async () => {
  loading.value = true
  try {
    await appointmentStore.fetchPatientAppointments()
  } finally {
    loading.value = false
  }
  appointmentStore.connectAppointmentSse()
})

onUnmounted(() => appointmentStore.disconnectAppointmentSse())
</script>

<style scoped>
.patient-appointments {
  max-width: 800px;
  margin: 0 auto;
  padding: 1rem;
}

.appointment-list {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.appointment-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 1rem;
  position: relative;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.doctor-name {
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.card-body {
  display: flex;
  gap: 1.5rem;
  color: #555;
}

.card-footer {
  margin-top: 0.5rem;
  text-align: right;
}

.created-at {
  color: #888;
  font-size: 0.75rem;
}

.badge {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
}

.badge-open {
  background-color: #dbeafe;
  color: #1d4ed8;
}

.badge-completed {
  background-color: #dcfce7;
  color: #15803d;
}

.badge-canceled {
  background-color: #fed7aa;
  color: #c2410c;
}

.badge-auto-canceled {
  background-color: #f3f4f6;
  color: #6b7280;
}

.badge-removed {
  background-color: #fee2e2;
  color: #b91c1c;
}

.rating-widget {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.rating-widget input {
  width: 4rem;
  padding: 2px 4px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.rating-widget button {
  padding: 2px 10px;
  border-radius: 4px;
  cursor: pointer;
}

.rating-error {
  color: #b91c1c;
  font-size: 0.85rem;
}

.empty-state {
  text-align: center;
  color: #888;
  margin-top: 3rem;
}
</style>
