<template>
  <div class="doctor-appointments">
    <h1>My Appointments</h1>

    <div class="tabs">
      <button
        :class="{ active: activeTab === 'upcoming' }"
        @click="activeTab = 'upcoming'"
      >Upcoming</button>
      <button
        :class="{ active: activeTab === 'past' }"
        @click="activeTab = 'past'"
      >Past</button>
    </div>

    <div v-if="loading" class="loading">Loading...</div>

    <div v-else-if="errorMessage" class="error">{{ errorMessage }}</div>

    <template v-else>
      <div v-if="activeList.length === 0" class="empty-state">
        No appointments.
      </div>

      <ul v-else class="appointment-list">
        <li
          v-for="appt in activeList"
          :key="appt.id"
          class="appointment-card"
        >
          <div class="card-header">
            <span class="patient-name">
              {{ appt.patientFirstName }} {{ appt.patientSurname }}
              <span v-if="appt.patientRemoved" class="badge badge-removed">Removed</span>
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
            <span>{{ appt.scheduledDate }} at {{ appt.startTime }}</span>
          </div>
          <div v-if="appt.status === 'OPEN'" class="card-actions">
            <button @click="cancelAppointment(appt.id)">Cancel</button>
            <button @click="completeAppointment(appt.id)">Complete</button>
            <button
              v-if="appt.wherebyRoomUrl"
              :disabled="!isJoinActive(appt.scheduledDate, appt.startTime, appt.status)"
              @click="joinConsultation(appt.wherebyRoomUrl)"
            >Join</button>
          </div>
          <div class="card-footer">
            <small class="created-at">Booked: {{ appt.createdAt }}</small>
          </div>
        </li>
      </ul>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAppointmentStore, isJoinActive } from '../stores/appointmentStore'

const appointmentStore = useAppointmentStore()
const loading = ref(false)
const errorMessage = ref('')
const activeTab = ref<'upcoming' | 'past'>('upcoming')

const upcoming = computed(() =>
  appointmentStore.doctorAppointments
    .filter(a => a.status === 'OPEN')
    .slice()
    .sort((a, b) => a.scheduledDate.localeCompare(b.scheduledDate) || a.startTime.localeCompare(b.startTime))
)

const past = computed(() =>
  appointmentStore.doctorAppointments
    .filter(a => a.status !== 'OPEN')
    .slice()
    .sort((a, b) => b.scheduledDate.localeCompare(a.scheduledDate) || b.startTime.localeCompare(a.startTime))
)

const activeList = computed(() => activeTab.value === 'upcoming' ? upcoming.value : past.value)

async function cancelAppointment(id: number) {
  await appointmentStore.updateDoctorAppointmentStatus(id, 'CANCELED')
}

async function completeAppointment(id: number) {
  await appointmentStore.updateDoctorAppointmentStatus(id, 'COMPLETED')
}

function joinConsultation(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer')
}

onMounted(async () => {
  loading.value = true
  try {
    await appointmentStore.fetchDoctorAppointments()
    appointmentStore.connectAppointmentSse()
  } catch {
    errorMessage.value = 'Failed to load appointments.'
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  appointmentStore.disconnectAppointmentSse()
})
</script>

<style scoped>
.doctor-appointments {
  max-width: 800px;
  margin: 0 auto;
  padding: 1rem;
}

.tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.tabs button {
  padding: 0.5rem 1.25rem;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: #f9f9f9;
  cursor: pointer;
}

.tabs button.active {
  background: #1d4ed8;
  color: #fff;
  border-color: #1d4ed8;
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

.patient-name {
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.card-body {
  color: #555;
  margin-bottom: 0.5rem;
}

.card-actions {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.card-actions button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.card-footer {
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
  background-color: #fef2f2;
  color: #dc2626;
}
</style>
