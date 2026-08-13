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
        <div class="card-footer">
          <small class="created-at">Booked: {{ appt.createdAt }}</small>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useAppointmentStore } from '../stores/appointmentStore'

const appointmentStore = useAppointmentStore()
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    await appointmentStore.fetchPatientAppointments()
  } finally {
    loading.value = false
  }
})
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

.empty-state {
  text-align: center;
  color: #888;
  margin-top: 3rem;
}
</style>
