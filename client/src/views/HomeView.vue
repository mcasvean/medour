<template>
  <VContainer class="py-8" max-width="900">
    <VRow>
      <VCol cols="12">
        <h1 class="text-h4 font-weight-bold mb-1">
          Good {{ timeOfDay }}, {{ authStore.user?.firstName || "" }} {{ authStore.user?.surname || "" }}!
        </h1>
        <p class="text-body-1 text-medium-emphasis mb-6">
          Welcome to Medour — your online medical appointment platform.
        </p>
      </VCol>
    </VRow>

    <!-- Patient quick actions -->
    <VRow v-if="authStore.user?.role === 'PATIENT'">
      <VCol cols="12" sm="6">
        <VCard rounded="xl" color="primary" variant="elevated" to="/booking" hover class="quick-card">
          <VCardText class="pa-6">
            <VIcon icon="mdi-magnify" size="44" color="white" class="mb-3" />
            <div class="text-h6 font-weight-bold text-white">Find a Doctor</div>
            <div class="text-body-2 text-white mt-1" style="opacity:0.85">
              Search by speciality, city or county and book a slot
            </div>
          </VCardText>
        </VCard>
      </VCol>
      <VCol cols="12" sm="6">
        <VCard rounded="xl" color="secondary" variant="elevated" to="/appointments" hover class="quick-card">
          <VCardText class="pa-6">
            <VIcon icon="mdi-calendar-check-outline" size="44" color="white" class="mb-3" />
            <div class="text-h6 font-weight-bold text-white">My Appointments</div>
            <div class="text-body-2 text-white mt-1" style="opacity:0.85">
              View status, join video calls, and rate your consultations
            </div>
          </VCardText>
        </VCard>
      </VCol>
    </VRow>

    <!-- Doctor quick actions -->
    <VRow v-else-if="authStore.user?.role === 'DOCTOR'">
      <VCol cols="12" sm="6">
        <VCard rounded="xl" color="success" variant="elevated" to="/appointments/doctor" hover class="quick-card">
          <VCardText class="pa-6">
            <VIcon icon="mdi-calendar-clock-outline" size="44" color="white" class="mb-3" />
            <div class="text-h6 font-weight-bold text-white">My Appointments</div>
            <div class="text-body-2 text-white mt-1" style="opacity:0.85">
              View upcoming consultations, join video calls, and manage appointments
            </div>
          </VCardText>
        </VCard>
      </VCol>
      <VCol cols="12" sm="6">
        <VCard rounded="xl" color="info" variant="elevated" to="/account" hover class="quick-card">
          <VCardText class="pa-6">
            <VIcon icon="mdi-account-edit-outline" size="44" color="white" class="mb-3" />
            <div class="text-h6 font-weight-bold text-white">My Profile</div>
            <div class="text-body-2 text-white mt-1" style="opacity:0.85">
              Update your speciality, location, and personal information
            </div>
          </VCardText>
        </VCard>
      </VCol>
    </VRow>

    <!-- Admin quick actions -->
    <VRow v-else-if="authStore.user?.role === 'ADMIN'">
      <VCol cols="12" sm="6">
        <VCard rounded="xl" color="primary" variant="elevated" to="/admin/users" hover class="quick-card">
          <VCardText class="pa-6">
            <VIcon icon="mdi-account-group-outline" size="44" color="white" class="mb-3" />
            <div class="text-h6 font-weight-bold text-white">Manage Users</div>
            <div class="text-body-2 text-white mt-1" style="opacity:0.85">
              View, add, edit and soft-delete users across all roles
            </div>
          </VCardText>
        </VCard>
      </VCol>
      <VCol cols="12" sm="6">
        <VCard rounded="xl" color="secondary" variant="elevated" to="/admin/appointments" hover class="quick-card">
          <VCardText class="pa-6">
            <VIcon icon="mdi-calendar-multiple" size="44" color="white" class="mb-3" />
            <div class="text-h6 font-weight-bold text-white">Manage Appointments</div>
            <div class="text-body-2 text-white mt-1" style="opacity:0.85">
              View and delete all appointments across the platform
            </div>
          </VCardText>
        </VCard>
      </VCol>
    </VRow>
  </VContainer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '../stores/authStore'

const authStore = useAuthStore()
const hour = new Date().getHours()
const timeOfDay = computed(() => hour < 12 ? 'morning' : hour < 17 ? 'afternoon' : 'evening')
</script>

<style scoped>
.quick-card {
  transition: transform 0.15s ease;
}
.quick-card:hover {
  transform: translateY(-2px);
}
</style>
