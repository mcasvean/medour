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
              <span><VIcon icon="mdi-calendar" size="15" class="mr-1" />{{ formatDate(appt.scheduledDate) }}</span>
              <span><VIcon icon="mdi-clock-outline" size="15" class="mr-1" />{{ formatTime(appt.startTime) }}</span>
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

              <VBtn
                v-if="appt.status === 'OPEN'"
                color="primary"
                variant="outlined"
                size="small"
                rounded="lg"
                prepend-icon="mdi-calendar-edit-outline"
                @click="openRescheduleDialog(appt)"
              >
                Reschedule
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
                  :disabled="!isRatingSaveable(appt.id, appt.ratingValue)"
                  @click="saveRating(appt.id)"
                >
                  Save Rating
                </VBtn>
                <span v-if="ratingErrors[appt.id]" class="text-error text-caption">
                  {{ ratingErrors[appt.id] }}
                </span>
              </div>

              <small class="text-caption text-medium-emphasis ml-auto">Booked: {{ formatBooked(appt.createdAt) }}</small>
            </div>
          </VCardText>
        </VCard>
      </VCol>
    </VRow>
  </VContainer>

  <!-- Reschedule dialog -->
  <VDialog v-model="rescheduleDialogOpen" max-width="560" persistent>
    <VCard rounded="xl">
      <VCardTitle class="pa-6 pb-3 text-h6">
        <VIcon icon="mdi-calendar-edit-outline" color="primary" class="mr-2" />
        Reschedule Appointment
      </VCardTitle>
      <VDivider />
      <VCardText class="pa-6">
        <VRow dense class="mb-4">
          <VCol cols="12" sm="8">
            <VTextField
              v-model="rescheduleDate"
              label="New Date"
              type="date"
              :min="today"
              variant="outlined"
              density="compact"
              hide-details
            />
          </VCol>
          <VCol cols="12" sm="4" class="d-flex align-center">
            <VBtn
              color="primary"
              variant="tonal"
              rounded="lg"
              block
              :disabled="!rescheduleDate"
              :loading="rescheduleLoadingSlots"
              @click="loadRescheduleSlots"
            >
              Load Slots
            </VBtn>
          </VCol>
        </VRow>

        <SlotGrid
          v-if="rescheduleSlots.length > 0"
          :slots="rescheduleSlots"
          @select="handleRescheduleSlotSelect"
        />

        <VAlert
          v-if="rescheduleDate && !rescheduleLoadingSlots && rescheduleSlots.length === 0 && rescheduleSlotsLoaded"
          type="info"
          variant="tonal"
          rounded="xl"
          class="mt-2"
        >
          No slots loaded yet — pick a date and click Load Slots.
        </VAlert>

        <div v-if="rescheduleSelectedTime" class="mt-4 text-body-2 text-medium-emphasis">
          Selected time: <strong class="text-primary">{{ formatTime(rescheduleSelectedTime) }}</strong>
        </div>
      </VCardText>
      <VCardActions class="pa-4 pt-0">
        <VSpacer />
        <VBtn variant="text" :disabled="rescheduleSubmitting" @click="closeRescheduleDialog">Cancel</VBtn>
        <VBtn
          color="primary"
          variant="elevated"
          rounded="lg"
          :disabled="!rescheduleSelectedTime"
          :loading="rescheduleSubmitting"
          @click="confirmReschedule"
        >
          Confirm Reschedule
        </VBtn>
      </VCardActions>
    </VCard>
  </VDialog>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useAppointmentStore, isJoinActive, type PatientAppointment, type SlotDisplay } from '../stores/appointmentStore'
import { formatBooked, formatDate, formatTime } from '../utils/formatDate'
import { useToastStore } from '../stores/toastStore'
import SlotGrid from '../components/SlotGrid.vue'
import api from '../api/index'

const appointmentStore = useAppointmentStore()
const toastStore = useToastStore()
const loading = ref(false)
const ratingInputs = ref<Record<number, number | null>>({})
const submittingRating = ref<Record<number, boolean>>({})
const ratingErrors = ref<Record<number, string>>({})

const today = new Date().toISOString().split('T')[0]

// reschedule dialog state
const rescheduleDialogOpen = ref(false)
const rescheduleTargetAppt = ref<PatientAppointment | null>(null)
const rescheduleDate = ref('')
const rescheduleSlots = ref<SlotDisplay[]>([])
const rescheduleSlotsLoaded = ref(false)
const rescheduleSelectedTime = ref<string | null>(null)
const rescheduleLoadingSlots = ref(false)
const rescheduleSubmitting = ref(false)

function openRescheduleDialog(appt: PatientAppointment) {
  rescheduleTargetAppt.value = appt
  rescheduleDate.value = ''
  rescheduleSlots.value = []
  rescheduleSlotsLoaded.value = false
  rescheduleSelectedTime.value = null
  rescheduleDialogOpen.value = true
}

function closeRescheduleDialog() {
  rescheduleDialogOpen.value = false
  rescheduleTargetAppt.value = null
}

async function loadRescheduleSlots() {
  if (!rescheduleTargetAppt.value || !rescheduleDate.value) return
  rescheduleLoadingSlots.value = true
  rescheduleSlots.value = []
  rescheduleSelectedTime.value = null
  try {
    const response = await api.get<SlotDisplay[]>(
      `/doctors/${rescheduleTargetAppt.value.doctorId}/slots`,
      { params: { date: rescheduleDate.value } },
    )
    rescheduleSlots.value = response.data
  } catch {
    toastStore.show('Failed to load available slots.', 'error')
  } finally {
    rescheduleLoadingSlots.value = false
    rescheduleSlotsLoaded.value = true
  }
}

function handleRescheduleSlotSelect(startTime: string) {
  rescheduleSelectedTime.value = startTime
}

async function confirmReschedule() {
  if (!rescheduleTargetAppt.value || !rescheduleDate.value || !rescheduleSelectedTime.value) return
  rescheduleSubmitting.value = true
  try {
    await appointmentStore.rescheduleAppointment(
      rescheduleTargetAppt.value.id,
      rescheduleDate.value,
      rescheduleSelectedTime.value,
    )
    toastStore.show('Appointment rescheduled successfully.', 'success')
    closeRescheduleDialog()
  } catch (err) {
    toastStore.showError(err)
  } finally {
    rescheduleSubmitting.value = false
  }
}

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

function isRatingSaveable(appointmentId: number, storedValue: number | null): boolean {
  const v = ratingInputs.value[appointmentId]
  if (v == null || Number.isNaN(v) || !Number.isInteger(v) || v < 1 || v > 10) return false
  return v !== storedValue
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

