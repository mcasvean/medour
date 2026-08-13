<template>
  <div class="admin-users">
    <h1>Users</h1>
    <div v-if="loading" class="loading">Loading...</div>
    <div v-else-if="errorMessage" class="error">{{ errorMessage }}</div>
    <ul v-else class="user-list">
      <li
        v-for="user in userStore.adminUsers"
        :key="user.id"
        class="user-row"
        :class="{ deleted: user.isDeleted }"
        @click="toggleExpand(user.id)"
      >
        <div class="user-summary">
          <span class="user-name">{{ user.firstName }} {{ user.surname }}</span>
          <span class="user-role">{{ user.role }}</span>
          <span v-if="user.isDeleted" class="deleted-badge">Deleted</span>
        </div>
        <div v-if="expandedUserId === user.id" class="user-detail" @click.stop>
          <dl>
            <dt>ID</dt><dd>{{ user.id }}</dd>
            <dt>Email</dt><dd>{{ user.email }}</dd>
            <dt>First name</dt><dd>{{ user.firstName }}</dd>
            <dt>Surname</dt><dd>{{ user.surname }}</dd>
            <dt>Role</dt><dd>{{ user.role }}</dd>
            <dt>Speciality</dt><dd>{{ user.speciality ?? '—' }}</dd>
            <dt>County</dt><dd>{{ user.county ?? '—' }}</dd>
            <dt>City</dt><dd>{{ user.city ?? '—' }}</dd>
            <dt>Age</dt><dd>{{ user.age ?? '—' }}</dd>
            <dt>Gender</dt><dd>{{ user.gender ?? '—' }}</dd>
            <dt>Address</dt><dd>{{ user.address ?? '—' }}</dd>
            <dt>Must change password</dt><dd>{{ user.mustChangePassword }}</dd>
            <dt>Status</dt><dd>{{ user.isDeleted ? 'Deleted' : 'Active' }}</dd>
          </dl>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '../stores/userStore'

const userStore = useUserStore()
const expandedUserId = ref<number | null>(null)
const loading = ref(false)
const errorMessage = ref('')

onMounted(async () => {
  loading.value = true
  try {
    await userStore.fetchAdminUsers()
  } catch {
    errorMessage.value = 'Failed to load users.'
  } finally {
    loading.value = false
  }
})

function toggleExpand(id: number) {
  expandedUserId.value = expandedUserId.value === id ? null : id
}
</script>

<style scoped>
.admin-users {
  padding: 1.5rem;
}

.user-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.user-row {
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-bottom: 0.5rem;
  padding: 0.75rem 1rem;
  cursor: pointer;
}

.user-row.deleted {
  opacity: 0.55;
  background-color: #f5f5f5;
}

.user-summary {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-name {
  font-weight: 600;
}

.user-role {
  color: #666;
  font-size: 0.875rem;
}

.deleted-badge {
  background: #e53e3e;
  color: #fff;
  font-size: 0.75rem;
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
}

.user-detail {
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid #eee;
}

.user-detail dl {
  display: grid;
  grid-template-columns: max-content 1fr;
  gap: 0.25rem 1rem;
  margin: 0;
}

.user-detail dt {
  font-weight: 600;
  color: #444;
}
</style>
