<template>
  <VApp>
    <VAppBar color="primary" elevation="2" density="comfortable">
      <template #prepend>
        <VAppBarNavIcon
          v-if="authStore.isAuthenticated"
          color="white"
          @click="drawer = !drawer"
        />
        <VIcon v-else icon="mdi-stethoscope" color="white" class="ml-3" />
      </template>
      <VAppBarTitle>
        <span
          class="text-white font-weight-bold text-h6"
          :style="authStore.isAuthenticated ? 'cursor:pointer' : ''"
          @click="authStore.isAuthenticated && router.push('/')"
        >Medour</span>
      </VAppBarTitle>
      <template #append>
        <div class="d-flex align-center ga-4 mr-1">
          <VBtn
            :icon="isDark ? 'mdi-weather-sunny' : 'mdi-weather-night'"
            color="white"
            variant="text"
            density="comfortable"
            @click="toggleTheme"
          />
          <template v-if="authStore.isAuthenticated">
            <VChip
              size="small"
              label
              variant="flat"
              color="white"
              class="font-weight-bold role-chip"
              :style="{ color: roleChipColor }"
            >
              {{ authStore.user?.role }}
            </VChip>
            <VAvatar v-if="authStore.user?.profilePicture" size="32" rounded="circle">
              <VImg :src="authStore.user.profilePicture" cover />
            </VAvatar>
            <span class="text-white text-body-2 d-none d-sm-inline font-weight-medium">
              {{ authStore.user?.firstName }} {{ authStore.user?.surname }}
            </span>
          </template>
        </div>
      </template>
    </VAppBar>

    <VNavigationDrawer v-if="authStore.isAuthenticated" v-model="drawer" :temporary="!pinned">
      <VListItem
        prepend-icon="mdi-stethoscope"
        title="Medour"
        subtitle="Medical Appointments"
        nav
        class="py-4"
      >
        <template #append>
          <VBtn
            :icon="pinned ? 'mdi-pin-off' : 'mdi-pin'"
            :title="pinned ? 'Unpin sidebar' : 'Pin sidebar'"
            variant="text"
            size="small"
            color="grey"
            @click="togglePin"
          />
        </template>
      </VListItem>
      <VDivider />
      <VList nav density="compact" class="mt-1">
        <VListItem
          prepend-icon="mdi-home-outline"
          title="Home"
          to="/"
          exact
          rounded="lg"
        />
        <VListItem
          v-if="authStore.user?.role === 'PATIENT'"
          prepend-icon="mdi-magnify"
          title="Book Appointment"
          to="/booking"
          rounded="lg"
        />
        <VListItem
          v-if="authStore.user?.role === 'PATIENT'"
          prepend-icon="mdi-calendar-check-outline"
          title="My Appointments"
          to="/appointments"
          rounded="lg"
        />
        <VListItem
          v-if="authStore.user?.role === 'DOCTOR'"
          prepend-icon="mdi-calendar-clock-outline"
          title="My Appointments"
          to="/appointments/doctor"
          rounded="lg"
        />
        <VDivider class="my-2" />
        <VListItem
          prepend-icon="mdi-account-outline"
          title="Account Info"
          to="/account"
          rounded="lg"
        />
        <VListItem
          prepend-icon="mdi-lock-reset"
          title="Change Password"
          to="/change-password"
          rounded="lg"
        />
        <template v-if="isAdmin">
          <VDivider class="my-2" />
          <VListSubheader>Admin</VListSubheader>
          <VListItem
            prepend-icon="mdi-account-group-outline"
            title="Users"
            to="/admin/users"
            rounded="lg"
          />
          <VListItem
            prepend-icon="mdi-tag-multiple-outline"
            title="Specialities"
            to="/admin/specialities"
            rounded="lg"
          />
          <VListItem
            prepend-icon="mdi-calendar-multiple"
            title="Appointments"
            to="/admin/appointments"
            rounded="lg"
          />
        </template>
        <VDivider class="my-2" />
        <VListItem
          prepend-icon="mdi-logout"
          title="Sign Out"
          rounded="lg"
          base-color="error"
          class="font-weight-medium"
          @click="logoutFromMenu"
        />
      </VList>
    </VNavigationDrawer>

    <VMain>
      <RouterView />
    </VMain>
    <ToastNotification />
  </VApp>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useTheme } from 'vuetify'
import { useAuthStore } from './stores/authStore'
import ToastNotification from './components/ToastNotification.vue'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const theme = useTheme()

// read before first render to avoid open-then-close flash
const _pinnedStored = (() => { try { return localStorage.getItem('medour_sidebar_pinned') === 'true' } catch { return false } })()
const drawer = ref(_pinnedStored)
const pinned = ref(_pinnedStored)
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')
const isDark = computed(() => theme.global.name.value === 'dark')

// High-contrast chip: white background, role-specific text colour on the primary bar
const roleChipColor = computed(() => {
  switch (authStore.user?.role) {
    case 'ADMIN': return '#FF5252'
    case 'DOCTOR': return '#00C853'
    default: return '#1565C0'
  }
})

watch(() => route.path, () => { if (!pinned.value) drawer.value = false })

// closing the drawer while pinned = user wants it unpinned; guard against logout triggering this
watch(drawer, (isOpen) => {
  if (!isOpen && pinned.value && authStore.isAuthenticated) {
    pinned.value = false
    try { localStorage.setItem('medour_sidebar_pinned', 'false') } catch { /* */ }
  }
})

onMounted(() => {
  try {
    const stored = localStorage.getItem('medour_theme')
    if (stored === 'dark' || stored === 'light') {
      theme.global.name.value = stored
    }
  } catch { /* ignore — storage unavailable (e.g. strict incognito) */ }
})

function toggleTheme() {
  const next = isDark.value ? 'light' : 'dark'
  theme.global.name.value = next
  try {
    localStorage.setItem('medour_theme', next)
  } catch { /* ignore — storage unavailable */ }
}

function togglePin() {
  pinned.value = !pinned.value
  if (pinned.value) drawer.value = true
  try {
    localStorage.setItem('medour_sidebar_pinned', String(pinned.value))
  } catch { /* ignore */ }
}

function logout() {
  authStore.clearAuth()
  router.push('/login')
}

function logoutFromMenu() {
  try {
    logout()
  } finally {
    drawer.value = false
  }
}
</script>

<style scoped>
.role-chip {
  font-size: 0.68rem;
  letter-spacing: 0.06em;
}
</style>
