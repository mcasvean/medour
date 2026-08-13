<template>
  <header v-if="authStore.isAuthenticated">
    <span>{{ authStore.user?.firstName }} ({{ authStore.user?.role }})</span>
    <button @click="menuOpen = !menuOpen">☰</button>
    <nav v-show="menuOpen">
      <RouterLink to="/account">Account Info</RouterLink>
      <RouterLink to="/change-password">Change Password</RouterLink>
      <RouterLink v-if="authStore.user?.role === 'DOCTOR'" to="/appointments/doctor">My Appointments</RouterLink>
      <RouterLink v-if="isAdmin" to="/admin/users">Users</RouterLink>
      <RouterLink v-if="isAdmin" to="/admin/appointments">Appointments</RouterLink>
      <button @click="logout">Logout</button>
    </nav>
  </header>
  <RouterView />
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from './stores/authStore'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const menuOpen = ref(false)
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

// close menu whenever the active route changes
watch(() => route.path, () => { menuOpen.value = false })

function logout() {
  menuOpen.value = false
  authStore.clearAuth()
  router.push('/login')
}
</script>
