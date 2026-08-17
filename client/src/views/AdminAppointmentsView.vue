<template>
  <VContainer class="py-6">
    <h1 class="text-h5 font-weight-bold mb-6">All Appointments</h1>

    <VAlert v-if="fetchError" type="error" variant="tonal" class="mb-4" rounded="xl">{{ fetchError }}</VAlert>
    <VAlert v-if="deleteError" type="error" variant="tonal" class="mb-4" rounded="xl">{{ deleteError }}</VAlert>

    <VCard rounded="xl" elevation="1">
      <VDataTable
        :headers="headers"
        :items="appointmentStore.adminAppointments"
        :loading="loading"
        item-value="id"
        hover
      >
        <template #item.status="{ item }">
          <VChip :color="statusColor(item.status)" size="small" label>{{ item.status }}</VChip>
        </template>
        <template #item.scheduledDate="{ item }">
          {{ formatDate(item.scheduledDate) }}
        </template>
        <template #item.startTime="{ item }">
          {{ formatTime(item.startTime) }}
        </template>
        <template #item.actions="{ item }">
          <VBtn
            icon="mdi-delete-outline"
            size="small"
            color="error"
            variant="text"
            @click="confirmDelete(item)"
          />
        </template>
        <template #no-data>
          <div class="text-center text-medium-emphasis py-8">
            <VIcon icon="mdi-calendar-blank-outline" size="48" class="mb-2 d-block" />
            No appointments found.
          </div>
        </template>
      </VDataTable>
    </VCard>

    <!-- Delete confirmation dialog -->
    <VDialog v-model="showDeleteDialog" max-width="420">
      <VCard rounded="xl">
        <VCardTitle class="pa-6 pb-2 text-h6">
          <VIcon icon="mdi-alert-circle-outline" color="error" class="mr-2" />Delete Appointment
        </VCardTitle>
        <VCardText class="pa-6 pt-2">
          Are you sure you want to delete the appointment for
          <strong>{{ apptToDelete?.patientName }}</strong>?
        </VCardText>
        <VCardActions class="pa-4 pt-0">
          <VSpacer />
          <VBtn variant="text" @click="showDeleteDialog = false">Cancel</VBtn>
          <VBtn color="error" variant="elevated" rounded="lg" @click="executeDelete">Delete</VBtn>
        </VCardActions>
      </VCard>
    </VDialog>
  </VContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAppointmentStore } from '../stores/appointmentStore'
import type { AdminAppointmentDto } from '../stores/appointmentStore'
import { formatDate, formatTime } from '../utils/formatDate'

const appointmentStore = useAppointmentStore()
const fetchError = ref('')
const deleteError = ref('')
const loading = ref(false)
const showDeleteDialog = ref(false)
const apptToDelete = ref<AdminAppointmentDto | null>(null)

const headers = [
  { title: 'Patient', key: 'patientName', sortable: true },
  { title: 'Doctor', key: 'doctorName', sortable: true },
  { title: 'Date', key: 'scheduledDate', sortable: true },
  { title: 'Time', key: 'startTime', sortable: false },
  { title: 'Status', key: 'status', sortable: true },
  { title: 'Actions', key: 'actions', sortable: false, align: 'end' as const },
]

function statusColor(status: string): string {
  switch (status) {
    case 'OPEN': return 'primary'
    case 'COMPLETED': return 'success'
    case 'CANCELED': return 'warning'
    case 'AUTO_CANCELED': return 'error'
    default: return 'default'
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await appointmentStore.fetchAdminAppointments()
  } catch {
    fetchError.value = 'Failed to load appointments.'
  } finally {
    loading.value = false
  }
})

function confirmDelete(appt: AdminAppointmentDto) {
  apptToDelete.value = appt
  showDeleteDialog.value = true
}

async function executeDelete() {
  if (!apptToDelete.value) return
  deleteError.value = ''
  showDeleteDialog.value = false
  try {
    await appointmentStore.deleteAdminAppointment(apptToDelete.value.id)
  } catch {
    deleteError.value = 'Failed to delete appointment.'
  } finally {
    apptToDelete.value = null
  }
}
</script>
