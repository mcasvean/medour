<template>
  <VContainer class="py-6" max-width="900">
    <h1 class="text-h5 font-weight-bold mb-6">My Appointments</h1>

    <div v-if="loading" class="d-flex justify-center py-12">
      <VProgressCircular indeterminate color="primary" size="56" />
    </div>

    <VAlert
      v-else-if="appointmentStore.patientAppointments.length === 0"
      type="info"
      variant="tonal"
      rounded="xl"
      icon="mdi-calendar-blank-outline"
    >
      You have no appointments yet.
      <VBtn to="/booking" color="info" variant="text" size="small" class="ml-1">Book one now</VBtn>
    </VAlert>

    <VRow v-else>
      <VCol
        v-for="appt in appointmentStore.patientAppointments"
        :key="appt.id"
        cols="12"
      >
        <VCard rounded="xl" elevation="1">
          <VCardText class="pa-5">
            <div class="d-flex justify-space-between align-start mb-3">
              <div class="d-flex align-center ga-3">
                <VAvatar color="primary" size="44">
                  <VIcon icon="mdi-doctor" size="24" />
                </VAvatar>
                <div>
                  <div class="text-subtitle-1 font-weight-bold">
                    {{ appt.doctorFirstName }} {{ appt.doctorSurname }}
                    <VChip v-if="appt.doctorRemoved" size="x-small" color="error" class="ml-1">Removed</VChip>
                  </div>
                  <div class="text-body-2 text-medium-emphasis">
                    <VIcon icon="mdi-medical-bag" size="13" class="mr-1" />{{ appt.doctorSpeciality }}
                  </div>
                </div>
              </div>
              <VChip :color="statusColor(appt.status)" size="small" label class="font-weight-medium">
                {{ appt.status }}
              </VChip>
            </div>

            <VDivider class="mb-3" />

            <div class="d-flex ga-5 text-body-2 text-medium-emphasis mb-3">
              <span><VIcon icon="mdi-calendar" size="15" class="mr-1" />{{ appt.scheduledDate }}</span>
              <span><VIcon icon="mdi-clock-outline" size="15" class="mr-1" />{{ appt.startTime }}</span>
            </div>

            <div class="d-flex justify-space-between align-center flex-wrap ga-2">
              <VBtn
                v-if="appt.status === 'OPEN' && appt.wherebyRoomUrl"
                color="success"
                variant="tonal"
                size="small"
                rounded="lg"
                prepend-icon="mdi-video-outline"
                :disabled="!isJoinActive(appt.scheduledDate, appt.startTime, appt.status)"
                @click="join(appt.wherebyRoomUrl!)"
              >
                Join Video Call
              </VBtn>

              <div v-if="appt.status === 'COMPLETED'" class="d-flex align-center ga-2">
                <VTextField
                  v-model.number="ratingInputs[appt.id]"
                  label="Rating (1–10)"
                  type="number"
                  min="1"
                  max="10"
                  density="compact"
                  variant="outlined"
                  style="max-width: 130px"
                  hide-details
                  :disabled="submittingRating[appt.id]"
                />
                <VBtn
                  size="small"
                  color="warning"
                  variant="tonal"
                  rounded="lg"
                  :loading="submittingRating[appt.id]"
                  @click="saveRating(appt.id)"
                >
                  Save Rating
                </VBtn>
                <span v-if="ratingErrors[appt.id]" class="text-error text-caption">
                  {{ ratingErrors[appt.id] }}
                </span>
              </div>

              <small class="text-caption text-medium-emphasis ml-auto">Booked: {{ appt.createdAt }}</small>
            </div>
          </VCardText>
        </VCard>
      </VCol>
    </VRow>
  </VContainer>
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

function statusColor(status: string): string {
  switch (status) {
    case 'OPEN': return 'primary'
    case 'COMPLETED': return 'success'
    case 'CANCELED': return 'warning'
    case 'AUTO_CANCELED': return 'error'
    default: return 'default'
  }
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

