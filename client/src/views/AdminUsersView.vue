<template>
  <VContainer class="py-6">
    <div class="d-flex justify-space-between align-center mb-6">
      <h1 class="text-h5 font-weight-bold">Users</h1>
      <VBtn color="primary" prepend-icon="mdi-account-plus" rounded="lg" @click="openCreate">Add User</VBtn>
    </div>

    <VAlert v-if="deleteError" type="error" variant="tonal" class="mb-4" rounded="xl">{{ deleteError }}</VAlert>
    <VAlert v-if="errorMessage" type="error" variant="tonal" class="mb-4" rounded="xl">{{ errorMessage }}</VAlert>

    <div v-if="loading" class="d-flex justify-center py-12">
      <VProgressCircular indeterminate color="primary" size="56" />
    </div>

    <template v-else>
      <!-- Admins section -->
      <template v-if="admins.length > 0">
        <div class="d-flex align-center ga-3 mb-4">
          <VIcon icon="mdi-shield-account-outline" color="error" />
          <h2 class="text-subtitle-1 font-weight-bold">Admins</h2>
          <VDivider class="flex-grow-1" />
        </div>
        <VRow class="mb-6">
          <VCol
            v-for="user in admins"
            :key="user.id"
            cols="12"
          >
            <VCard rounded="xl" elevation="1" :class="{ 'opacity-50': user.isDeleted }">
              <VCardText class="pa-4">
                <div class="d-flex justify-space-between align-start mb-3">
                  <div class="d-flex align-center ga-3">
                    <VAvatar :color="roleColor(user.role)" size="44">
                      <VIcon :icon="roleIcon(user.role)" size="24" />
                    </VAvatar>
                    <div>
                      <div class="text-subtitle-2 font-weight-bold">{{ user.firstName }} {{ user.surname }}</div>
                      <div class="text-caption text-medium-emphasis">{{ user.email }}</div>
                    </div>
                  </div>
                  <div class="d-flex flex-column align-end ga-1">
                    <VChip :color="roleColor(user.role)" size="x-small" label>{{ user.role }}</VChip>
                    <VChip v-if="user.isDeleted" color="error" size="x-small" label>Deleted</VChip>
                  </div>
                </div>
                <VExpansionPanels flat variant="accordion">
                  <VExpansionPanel elevation="0" rounded="lg" bg-color="surface-variant">
                    <VExpansionPanelTitle class="text-caption text-medium-emphasis py-2 px-3">View details</VExpansionPanelTitle>
                    <VExpansionPanelText class="px-0">
                      <VList density="compact" lines="two" bg-color="transparent">
                        <VListItem v-if="user.specialityName" title="Speciality" :subtitle="user.specialityName" density="compact" />
                        <VListItem v-if="user.county" title="County" :subtitle="user.county" density="compact" />
                        <VListItem v-if="user.city" title="City" :subtitle="user.city" density="compact" />
                        <VListItem title="Age" :subtitle="user.age ? String(user.age) : '—'" density="compact" />
                        <VListItem title="Gender" :subtitle="user.gender ?? '—'" density="compact" />
                        <VListItem title="Address" :subtitle="user.address ?? '—'" density="compact" />
                      </VList>
                    </VExpansionPanelText>
                  </VExpansionPanel>
                </VExpansionPanels>
                <div class="d-flex ga-2 mt-3">
                  <VBtn size="small" variant="tonal" color="primary" prepend-icon="mdi-pencil-outline" rounded="lg" @click="openEdit(user)">Edit</VBtn>
                  <VBtn size="small" variant="tonal" color="error" prepend-icon="mdi-delete-outline" rounded="lg" @click="confirmDelete(user)">Delete</VBtn>
                </div>
              </VCardText>
            </VCard>
          </VCol>
        </VRow>
      </template>

      <!-- Doctors section -->
      <template v-if="userStore.adminUsers.some(u => u.role === 'DOCTOR')">
        <div class="d-flex align-center ga-3 mb-4">
          <VIcon icon="mdi-doctor" color="success" />
          <h2 class="text-subtitle-1 font-weight-bold">Doctors</h2>
          <VDivider class="flex-grow-1" />
        </div>
        <VTextField
          v-model="doctorSearch"
          label="Search doctors…"
          prepend-inner-icon="mdi-magnify"
          variant="outlined"
          density="compact"
          clearable
          hide-details
          style="max-width: 320px"
          class="mb-4"
        />
        <p v-if="filteredDoctors.length === 0" class="text-caption text-medium-emphasis mb-6">No users found.</p>
        <VRow v-else class="mb-6">
          <VCol
            v-for="user in filteredDoctors"
            :key="user.id"
            cols="12"
            md="6"
            lg="4"
          >
            <VCard rounded="xl" elevation="1" :class="{ 'opacity-50': user.isDeleted }">
              <VCardText class="pa-4">
                <div class="d-flex justify-space-between align-start mb-3">
                  <div class="d-flex align-center ga-3">
                    <VAvatar :color="roleColor(user.role)" size="44">
                      <VIcon :icon="roleIcon(user.role)" size="24" />
                    </VAvatar>
                    <div>
                      <div class="text-subtitle-2 font-weight-bold">{{ user.firstName }} {{ user.surname }}</div>
                      <div class="text-caption text-medium-emphasis">{{ user.email }}</div>
                    </div>
                  </div>
                  <div class="d-flex flex-column align-end ga-1">
                    <VChip :color="roleColor(user.role)" size="x-small" label>{{ user.role }}</VChip>
                    <VChip v-if="user.isDeleted" color="error" size="x-small" label>Deleted</VChip>
                  </div>
                </div>
                <VExpansionPanels flat variant="accordion">
                  <VExpansionPanel elevation="0" rounded="lg" bg-color="surface-variant">
                    <VExpansionPanelTitle class="text-caption text-medium-emphasis py-2 px-3">View details</VExpansionPanelTitle>
                    <VExpansionPanelText class="px-0">
                      <VList density="compact" lines="two" bg-color="transparent">
                        <VListItem v-if="user.specialityName" title="Speciality" :subtitle="user.specialityName" density="compact" />
                        <VListItem v-if="user.county" title="County" :subtitle="user.county" density="compact" />
                        <VListItem v-if="user.city" title="City" :subtitle="user.city" density="compact" />
                        <VListItem title="Age" :subtitle="user.age ? String(user.age) : '—'" density="compact" />
                        <VListItem title="Gender" :subtitle="user.gender ?? '—'" density="compact" />
                        <VListItem title="Address" :subtitle="user.address ?? '—'" density="compact" />
                      </VList>
                    </VExpansionPanelText>
                  </VExpansionPanel>
                </VExpansionPanels>
                <div class="d-flex ga-2 mt-3">
                  <VBtn size="small" variant="tonal" color="primary" prepend-icon="mdi-pencil-outline" rounded="lg" @click="openEdit(user)">Edit</VBtn>
                  <VBtn size="small" variant="tonal" color="error" prepend-icon="mdi-delete-outline" rounded="lg" @click="confirmDelete(user)">Delete</VBtn>
                </div>
              </VCardText>
            </VCard>
          </VCol>
        </VRow>
      </template>

      <!-- Patients section -->
      <template v-if="userStore.adminUsers.some(u => u.role === 'PATIENT')">
        <div class="d-flex align-center ga-3 mb-4">
          <VIcon icon="mdi-account-heart-outline" color="info" />
          <h2 class="text-subtitle-1 font-weight-bold">Patients</h2>
          <VDivider class="flex-grow-1" />
        </div>
        <VTextField
          v-model="patientSearch"
          label="Search patients…"
          prepend-inner-icon="mdi-magnify"
          variant="outlined"
          density="compact"
          clearable
          hide-details
          style="max-width: 320px"
          class="mb-4"
        />
        <p v-if="filteredPatients.length === 0" class="text-caption text-medium-emphasis mb-6">No users found.</p>
        <VRow v-else class="mb-6">
          <VCol
            v-for="user in filteredPatients"
            :key="user.id"
            cols="12"
            md="6"
            lg="4"
          >
            <VCard rounded="xl" elevation="1" :class="{ 'opacity-50': user.isDeleted }">
              <VCardText class="pa-4">
                <div class="d-flex justify-space-between align-start mb-3">
                  <div class="d-flex align-center ga-3">
                    <VAvatar :color="roleColor(user.role)" size="44">
                      <VIcon :icon="roleIcon(user.role)" size="24" />
                    </VAvatar>
                    <div>
                      <div class="text-subtitle-2 font-weight-bold">{{ user.firstName }} {{ user.surname }}</div>
                      <div class="text-caption text-medium-emphasis">{{ user.email }}</div>
                    </div>
                  </div>
                  <div class="d-flex flex-column align-end ga-1">
                    <VChip :color="roleColor(user.role)" size="x-small" label>{{ user.role }}</VChip>
                    <VChip v-if="user.isDeleted" color="error" size="x-small" label>Deleted</VChip>
                  </div>
                </div>
                <VExpansionPanels flat variant="accordion">
                  <VExpansionPanel elevation="0" rounded="lg" bg-color="surface-variant">
                    <VExpansionPanelTitle class="text-caption text-medium-emphasis py-2 px-3">View details</VExpansionPanelTitle>
                    <VExpansionPanelText class="px-0">
                      <VList density="compact" lines="two" bg-color="transparent">
                        <VListItem v-if="user.specialityName" title="Speciality" :subtitle="user.specialityName" density="compact" />
                        <VListItem v-if="user.county" title="County" :subtitle="user.county" density="compact" />
                        <VListItem v-if="user.city" title="City" :subtitle="user.city" density="compact" />
                        <VListItem title="Age" :subtitle="user.age ? String(user.age) : '—'" density="compact" />
                        <VListItem title="Gender" :subtitle="user.gender ?? '—'" density="compact" />
                        <VListItem title="Address" :subtitle="user.address ?? '—'" density="compact" />
                      </VList>
                    </VExpansionPanelText>
                  </VExpansionPanel>
                </VExpansionPanels>
                <div class="d-flex ga-2 mt-3">
                  <VBtn size="small" variant="tonal" color="primary" prepend-icon="mdi-pencil-outline" rounded="lg" @click="openEdit(user)">Edit</VBtn>
                  <VBtn size="small" variant="tonal" color="error" prepend-icon="mdi-delete-outline" rounded="lg" @click="confirmDelete(user)">Delete</VBtn>
                </div>
              </VCardText>
            </VCard>
          </VCol>
        </VRow>
      </template>
    </template>

    <!-- Add/Edit user dialog -->
    <VDialog v-model="showForm" max-width="560" persistent scrollable>
      <AdminUserForm
        v-if="showForm"
        :key="editingUser?.id ?? 'new'"
        :mode="formMode"
        :user="editingUser ?? undefined"
        :save-error="formSaveError"
        @save="handleSave"
        @cancel="showForm = false; formSaveError = ''"
      />
    </VDialog>

    <!-- Delete confirmation dialog -->
    <VDialog v-model="showDeleteDialog" max-width="420">
      <VCard rounded="xl">
        <VCardTitle class="pa-6 pb-2 text-h6">
          <VIcon icon="mdi-alert-circle-outline" color="error" class="mr-2" />Delete User
        </VCardTitle>
        <VCardText class="pa-6 pt-2">
          Are you sure you want to delete
          <strong>{{ userToDelete?.firstName }} {{ userToDelete?.surname }}</strong>?
          This user will be soft-deleted and cannot log in.
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
import { ref, computed, onMounted } from 'vue'
import { useUserStore, type AdminUser } from '../stores/userStore'
import { useToastStore } from '../stores/toastStore'
import AdminUserForm from '../components/AdminUserForm.vue'

const userStore = useUserStore()
const toastStore = useToastStore()
const loading = ref(false)
const errorMessage = ref('')
const doctorSearch = ref('')
const patientSearch = ref('')

const admins = computed(() => userStore.adminUsers.filter(u => u.role === 'ADMIN'))

const filteredDoctors = computed(() => {
  const q = doctorSearch.value.toLowerCase()
  return userStore.adminUsers
    .filter(u => u.role === 'DOCTOR')
    .filter(u => !q || u.firstName.toLowerCase().includes(q) || u.surname.toLowerCase().includes(q))
})

const filteredPatients = computed(() => {
  const q = patientSearch.value.toLowerCase()
  return userStore.adminUsers
    .filter(u => u.role === 'PATIENT')
    .filter(u => !q || u.firstName.toLowerCase().includes(q) || u.surname.toLowerCase().includes(q))
})

const showForm = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingUser = ref<AdminUser | null>(null)
const formSaveError = ref('')
const deleteError = ref('')
const showDeleteDialog = ref(false)
const userToDelete = ref<AdminUser | null>(null)

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

function roleColor(role: string): string {
  switch (role) {
    case 'ADMIN': return 'error'
    case 'DOCTOR': return 'success'
    default: return 'info'
  }
}

function roleIcon(role: string): string {
  switch (role) {
    case 'ADMIN': return 'mdi-shield-account-outline'
    case 'DOCTOR': return 'mdi-doctor'
    default: return 'mdi-account-heart-outline'
  }
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

function confirmDelete(user: AdminUser) {
  userToDelete.value = user
  showDeleteDialog.value = true
}

async function executeDelete() {
  if (!userToDelete.value) return
  deleteError.value = ''
  showDeleteDialog.value = false
  try {
    await userStore.deleteAdminUser(userToDelete.value.id)
  } catch {
    deleteError.value = 'Failed to delete user.'
  } finally {
    userToDelete.value = null
  }
}

async function handleSave(payload: Partial<AdminUser> & { password?: string; newPassword?: string }) {
  formSaveError.value = ''
  if (formMode.value === 'create') {
    try {
      await userStore.createAdminUser(payload as Parameters<typeof userStore.createAdminUser>[0])
      showForm.value = false
    } catch {
      formSaveError.value = 'Failed to save user.'
    }
  } else if (editingUser.value) {
    const userId = editingUser.value.id
    const { newPassword, ...profilePayload } = payload
    try {
      await userStore.updateAdminUser(userId, profilePayload)
      toastStore.show('User profile updated.', 'success')
    } catch {
      formSaveError.value = 'Failed to save user.'
      return
    }
    if (newPassword) {
      try {
        await userStore.resetAdminUserPassword(userId, newPassword)
        toastStore.show('Password reset successfully.', 'success')
      } catch (err) {
        toastStore.showError(err)
        return
      }
    }
    showForm.value = false
  }
}
</script>
