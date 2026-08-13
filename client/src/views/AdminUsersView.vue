<template>
  <div class="admin-users">
    <div class="admin-header">
      <h1>Users</h1>
      <button class="btn-add" @click="openCreate">Add User</button>
    </div>
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
          <button class="btn-edit" @click.stop="openEdit(user)">Edit</button>
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

    <AdminUserForm
      v-if="showForm"
      :key="editingUser?.id ?? 'new'"
      :mode="formMode"
      :user="editingUser ?? undefined"
      :save-error="formSaveError"
      @save="handleSave"
      @cancel="showForm = false; formSaveError = ''"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore, type AdminUser } from '../stores/userStore'
import AdminUserForm from '../components/AdminUserForm.vue'

const userStore = useUserStore()
const expandedUserId = ref<number | null>(null)
const loading = ref(false)
const errorMessage = ref('')

const showForm = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingUser = ref<AdminUser | null>(null)
const formSaveError = ref('')

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

function openCreate() {
  editingUser.value = null
  formMode.value = 'create'
  showForm.value = true
}

function openEdit(user: AdminUser) {
  editingUser.value = user
  formMode.value = 'edit'
  showForm.value = true
}

async function handleSave(payload: Partial<AdminUser> & { password?: string }) {
  formSaveError.value = ''
  try {
    if (formMode.value === 'create') {
      await userStore.createAdminUser(payload as Parameters<typeof userStore.createAdminUser>[0])
    } else if (editingUser.value) {
      await userStore.updateAdminUser(editingUser.value.id, payload)
    }
    showForm.value = false
    formSaveError.value = ''
  } catch {
    formSaveError.value = 'Failed to save user.'
  }
}
</script>

<style scoped>
.admin-users {
  padding: 1.5rem;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.admin-header h1 {
  margin: 0;
}

.btn-add {
  background: #2b6cb0;
  color: #fff;
  border: none;
  border-radius: 4px;
  padding: 0.4rem 1rem;
  cursor: pointer;
  font-size: 0.875rem;
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

.btn-edit {
  margin-left: auto;
  padding: 0.2rem 0.6rem;
  font-size: 0.8rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  cursor: pointer;
  background: #fff;
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
