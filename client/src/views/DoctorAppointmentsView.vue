<template>
  <VContainer class="py-6" max-width="900">
    <h1 class="text-h5 font-weight-bold mb-4">My Appointments</h1>

    <VTabs v-model="activeTab" color="primary" class="mb-5" density="comfortable">
      <VTab value="upcoming">
        <VIcon start icon="mdi-calendar-clock-outline" />Upcoming
      </VTab>
      <VTab value="past">
        <VIcon start icon="mdi-calendar-check-outline" />Past
      </VTab>
    </VTabs>

    <div v-if="loading" class="d-flex justify-center py-12">
      <VProgressCircular indeterminate color="primary" size="56" />
    </div>

    <VAlert v-else-if="errorMessage" type="error" variant="tonal" rounded="xl">
      {{ errorMessage }}
    </VAlert>

    <template v-else>
      <VAlert
        v-if="activeList.length === 0"
        type="info"
        variant="tonal"
        rounded="xl"
        icon="mdi-calendar-blank-outline"
      >
        No appointments in this category.
      </VAlert>

      <VRow v-else>
        <VCol v-for="appt in activeList" :key="appt.id" cols="12">
          <VCard rounded="xl" elevation="1">
            <VCardText class="pa-5">
              <div class="d-flex justify-space-between align-start mb-3">
                <div class="d-flex align-center ga-3">
                  <VAvatar color="info" size="44">
                    <VIcon icon="mdi-account-outline" size="24" />
                  </VAvatar>
                  <div>
                    <div class="text-subtitle-1 font-weight-bold">
                      {{ appt.patientFirstName }} {{ appt.patientSurname }}
                      <VChip v-if="appt.patientRemoved" size="x-small" color="error" class="ml-1">Removed</VChip>
                    </div>
                    <div class="text-body-2 text-medium-emphasis">Patient</div>
                  </div>
                </div>
                <VChip :color="statusColor(appt.status)" size="small" label class="font-weight-medium">
                  {{ appt.status }}
                </VChip>
              </div>

              <VDivider class="mb-3" />

              <div class="d-flex justify-space-between align-center flex-wrap ga-2">
                <div class="d-flex ga-5 text-body-2 text-medium-emphasis">
                  <span><VIcon icon="mdi-calendar" size="15" class="mr-1" />{{ appt.scheduledDate }}</span>
                  <span><VIcon icon="mdi-clock-outline" size="15" class="mr-1" />{{ appt.startTime }}</span>
                </div>
                <div v-if="appt.status === 'OPEN'" class="d-flex ga-2">
                  <VBtn
                    v-if="appt.wherebyRoomUrl"
                    color="success"
                    size="small"
                    variant="tonal"
                    rounded="lg"
                    prepend-icon="mdi-video-outline"
                    :disabled="!isJoinActive(appt.scheduledDate, appt.startTime, appt.status)"
                    @click="joinConsultation(appt.wherebyRoomUrl)"
                  >
                    Join
                  </VBtn>
                  <VBtn
                    size="small"
                    color="error"
                    variant="tonal"
                    rounded="lg"
                    prepend-icon="mdi-close-circle-outline"
                    @click="cancelAppointment(appt.id)"
                  >
                    Cancel
                  </VBtn>
                  <VBtn
                    size="small"
                    color="success"
                    variant="tonal"
                    rounded="lg"
                    prepend-icon="mdi-check-circle-outline"
                    @click="completeAppointment(appt.id)"
                  >
                    Complete
                  </VBtn>
                </div>
                <small class="text-caption text-medium-emphasis ml-auto">Booked: {{ formatBooked(appt.createdAt) }}</small>
              </div>
            </VCardText>
          </VCard>
        </VCol>
      </VRow>
    </template>
  </VContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAppointmentStore, isJoinActive } from '../stores/appointmentStore'
import { formatBooked } from '../utils/formatDate'

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

function statusColor(status: string): string {
  switch (status) {
    case 'OPEN': return 'primary'
    case 'COMPLETED': return 'success'
    case 'CANCELED': return 'warning'
    case 'AUTO_CANCELED': return 'error'
    default: return 'default'
  }
}

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

