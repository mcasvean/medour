<template>
  <div>
    <h2>All Appointments</h2>
    <p v-if="fetchError" class="error">{{ fetchError }}</p>
    <p v-if="deleteError" class="error">{{ deleteError }}</p>
    <table v-if="appointmentStore.adminAppointments.length > 0">
      <thead>
        <tr>
          <th>Patient</th>
          <th>Doctor</th>
          <th>Date</th>
          <th>Time</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="appt in appointmentStore.adminAppointments" :key="appt.id">
          <td>{{ appt.patientName }}</td>
          <td>{{ appt.doctorName }}</td>
          <td>{{ appt.scheduledDate }}</td>
          <td>{{ appt.startTime }}</td>
          <td>{{ appt.status }}</td>
          <td>
            <button @click="deleteAppointment(appt)">Delete</button>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-else>No appointments found.</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAppointmentStore } from '../stores/appointmentStore'
import type { AdminAppointmentDto } from '../stores/appointmentStore'

const appointmentStore = useAppointmentStore()
const fetchError = ref('')
const deleteError = ref('')

onMounted(async () => {
  try {
    await appointmentStore.fetchAdminAppointments()
  } catch {
    fetchError.value = 'Failed to load appointments.'
  }
})

async function deleteAppointment(appt: AdminAppointmentDto) {
  if (!window.confirm('Delete appointment for ' + appt.patientName + '?')) return
  deleteError.value = ''
  try {
    await appointmentStore.deleteAdminAppointment(appt.id)
  } catch {
    deleteError.value = 'Failed to delete appointment.'
  }
}
</script>

<style scoped>
.error {
  color: red;
}
</style>
