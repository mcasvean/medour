<template>
  <VContainer class="py-6" max-width="1100">
    <h1 class="text-h5 font-weight-bold mb-5">Find a Doctor</h1>

    <!-- Search filters -->
    <VCard rounded="xl" elevation="1" class="mb-6">
      <VCardText class="pa-4">
        <VForm @submit.prevent="doctorStore.searchDoctors()">
          <VRow align="center" dense>
            <VCol cols="12" sm="6" md="3">
              <VTextField
                v-model="doctorStore.filters.speciality"
                label="Speciality"
                prepend-inner-icon="mdi-medical-bag"
                variant="outlined"
                density="compact"
                clearable
                hide-details
              />
            </VCol>
            <VCol cols="12" sm="6" md="3">
              <VTextField
                v-model="doctorStore.filters.county"
                label="County"
                prepend-inner-icon="mdi-map-outline"
                variant="outlined"
                density="compact"
                clearable
                hide-details
              />
            </VCol>
            <VCol cols="12" sm="6" md="2">
              <VTextField
                v-model="doctorStore.filters.city"
                label="City"
                prepend-inner-icon="mdi-city-outline"
                variant="outlined"
                density="compact"
                clearable
                hide-details
              />
            </VCol>
            <VCol cols="12" sm="6" md="3">
              <VTextField
                v-model="doctorStore.filters.date"
                label="Date"
                type="date"
                :min="today"
                variant="outlined"
                density="compact"
                hide-details
              />
            </VCol>
            <VCol cols="12" md="1" class="d-flex justify-end">
              <VBtn
                type="submit"
                color="primary"
                :loading="doctorStore.loading"
                icon="mdi-magnify"
                rounded="lg"
              />
            </VCol>
          </VRow>
        </VForm>
      </VCardText>
    </VCard>

    <!-- Loading -->
    <div v-if="doctorStore.loading" class="d-flex justify-center py-12">
      <VProgressCircular indeterminate color="primary" size="56" />
    </div>

    <!-- Empty -->
    <VAlert
      v-else-if="doctorStore.doctors.length === 0"
      type="info"
      variant="tonal"
      rounded="xl"
      icon="mdi-doctor"
    >
      No doctors found. Try adjusting your search filters.
    </VAlert>

    <!-- Doctor cards -->
    <VRow v-else>
      <VCol
        v-for="doctor in doctorStore.doctors"
        :key="doctor.id"
        cols="12"
        sm="6"
        md="4"
      >
        <VCard
          rounded="xl"
          elevation="1"
          hover
          :class="{ 'border-primary border-opacity-100': appointmentStore.selectedDoctorId === doctor.id }"
          :border="appointmentStore.selectedDoctorId === doctor.id ? 'primary sm' : false"
        >
          <VCardText class="pa-4">
            <div class="d-flex justify-space-between align-start mb-3">
              <div>
                <div class="text-subtitle-1 font-weight-bold">
                  {{ doctor.firstName }} {{ doctor.surname }}
                </div>
                <div class="text-body-2 text-medium-emphasis">
                  <VIcon icon="mdi-medical-bag" size="14" class="mr-1" />{{ doctor.speciality }}
                </div>
                <div class="text-body-2 text-medium-emphasis">
                  <VIcon icon="mdi-map-marker-outline" size="14" class="mr-1" />{{ doctor.county }}, {{ doctor.city }}
                </div>
              </div>
              <VChip
                v-if="doctor.averageRating !== null"
                :color="ratingColor(doctor.averageRating)"
                size="small"
                label
              >
                <VIcon start icon="mdi-star" size="14" />{{ doctor.averageRating.toFixed(1) }}
              </VChip>
            </div>
            <VBtn
              color="primary"
              :variant="appointmentStore.selectedDoctorId === doctor.id ? 'elevated' : 'tonal'"
              block
              rounded="lg"
              @click="selectDoctor(doctor.id)"
            >
              {{ appointmentStore.selectedDoctorId === doctor.id ? 'Selected ✓' : 'View Slots' }}
            </VBtn>
          </VCardText>
        </VCard>
      </VCol>
    </VRow>

    <!-- Slot picker -->
    <template v-if="appointmentStore.selectedDoctorId !== null">
      <VDivider class="my-6" />
      <div class="d-flex align-center mb-4">
        <h2 class="text-h6 font-weight-bold">
          Available Slots
          <span class="text-primary">— {{ selectedDoctor?.firstName }} {{ selectedDoctor?.surname }}</span>
        </h2>
        <VChip class="ml-3" size="small" variant="tonal">{{ appointmentStore.selectedDate }}</VChip>
      </div>
      <SlotGrid :slots="appointmentStore.slots" @select="onSlotSelected" />
    </template>

    <VAlert
      v-if="appointmentStore.errorMessage"
      type="error"
      variant="tonal"
      class="mt-4"
      rounded="xl"
    >
      {{ appointmentStore.errorMessage }}
    </VAlert>

    <!-- Booking success -->
    <VAlert
      v-if="appointmentStore.bookingStep === 'done'"
      type="success"
      variant="tonal"
      class="mt-4"
      rounded="xl"
      icon="mdi-check-circle-outline"
    >
      <div class="font-weight-bold">Appointment booked successfully!</div>
      <div v-if="appointmentStore.wherebyRoomUrl" class="mt-1 text-body-2">
        Video room:
        <a :href="appointmentStore.wherebyRoomUrl" target="_blank" rel="noopener" class="text-success">
          {{ appointmentStore.wherebyRoomUrl }}
        </a>
      </div>
    </VAlert>

    <!-- Confirmation dialog -->
    <VDialog v-model="showConfirmDialog" max-width="480" persistent>
      <VCard rounded="xl">
        <VCardTitle class="pa-6 pb-3 text-h6">
          <VIcon icon="mdi-calendar-check-outline" color="primary" class="mr-2" />
          Confirm Appointment
        </VCardTitle>
        <VDivider />
        <VCardText class="pa-6">
          <VList density="compact" lines="two" bg-color="transparent">
            <VListItem prepend-icon="mdi-doctor">
              <VListItemTitle class="font-weight-medium">
                {{ selectedDoctor?.firstName }} {{ selectedDoctor?.surname }}
              </VListItemTitle>
              <VListItemSubtitle>Doctor</VListItemSubtitle>
            </VListItem>
            <VListItem prepend-icon="mdi-calendar">
              <VListItemTitle class="font-weight-medium">{{ appointmentStore.selectedDate }}</VListItemTitle>
              <VListItemSubtitle>Date</VListItemSubtitle>
            </VListItem>
            <VListItem prepend-icon="mdi-clock-outline">
              <VListItemTitle class="font-weight-medium">{{ appointmentStore.lockedStartTime }}</VListItemTitle>
              <VListItemSubtitle>Start Time</VListItemSubtitle>
            </VListItem>
          </VList>
        </VCardText>
        <VCardActions class="pa-4 pt-0">
          <VSpacer />
          <VBtn variant="text" @click="appointmentStore.cancelBooking()">Cancel</VBtn>
          <VBtn color="primary" variant="elevated" rounded="lg" @click="appointmentStore.confirmBooking()">
            Confirm Booking
          </VBtn>
        </VCardActions>
      </VCard>
    </VDialog>
  </VContainer>
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

const showConfirmDialog = computed(() => appointmentStore.bookingStep === 'confirming')

function ratingColor(rating: number): string {
  if (rating >= 8) return 'success'
  if (rating >= 5) return 'info'
  return 'warning'
}

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
