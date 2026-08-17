<template>
  <VContainer class="py-8" max-width="800">
    <h1 class="text-h5 font-weight-bold mb-6">Speciality Management</h1>

    <!-- Add new speciality -->
    <VCard rounded="xl" elevation="1" class="mb-6">
      <VCardText class="pa-5">
        <h2 class="text-subtitle-1 font-weight-bold mb-4">Add Speciality</h2>
        <VForm @submit.prevent="handleAdd">
          <div class="d-flex ga-3 align-start">
            <VTextField
              v-model="newName"
              label="Speciality name"
              variant="outlined"
              density="compact"
              hide-details="auto"
              :error-messages="addError ? [addError] : []"
              class="flex-grow-1"
            />
            <VBtn type="submit" color="primary" :loading="adding" rounded="lg">Add</VBtn>
          </div>
        </VForm>
      </VCardText>
    </VCard>

    <!-- List -->
    <VCard rounded="xl" elevation="1">
      <VCardText class="pa-0">
        <div v-if="loading" class="d-flex justify-center py-8">
          <VProgressCircular indeterminate color="primary" size="48" />
        </div>

        <VDataTable
          v-else
          :headers="headers"
          :items="specialityStore.specialities"
          item-value="id"
          hide-default-footer
          :items-per-page="-1"
        >
          <template #item.name="{ item }">
            <div v-if="editingId === item.id" class="d-flex align-center ga-2 py-1">
              <VTextField
                v-model="editName"
                variant="outlined"
                density="compact"
                hide-details="auto"
                :error-messages="editError ? [editError] : []"
                autofocus
                style="min-width: 200px"
              />
              <VBtn
                color="primary"
                size="small"
                variant="elevated"
                rounded="lg"
                :loading="saving"
                @click="handleSave(item.id)"
              >
                Save
              </VBtn>
              <VBtn size="small" variant="text" @click="cancelEdit">Cancel</VBtn>
            </div>
            <span v-else>{{ item.name }}</span>
          </template>

          <template #item.actions="{ item }">
            <div class="d-flex ga-1 justify-end">
              <VBtn
                v-if="editingId !== item.id"
                icon="mdi-pencil-outline"
                size="small"
                variant="text"
                @click="startEdit(item)"
              />
              <VBtn
                icon="mdi-delete-outline"
                size="small"
                variant="text"
                color="error"
                @click="confirmDelete(item)"
              />
            </div>
          </template>

          <template #no-data>
            <div class="text-center text-medium-emphasis py-6">
              No specialities yet. Add one above.
            </div>
          </template>
        </VDataTable>
      </VCardText>
    </VCard>

    <!-- Delete confirmation dialog -->
    <VDialog v-model="deleteDialog" max-width="420">
      <VCard rounded="xl">
        <VCardTitle class="pa-5 pb-2">Delete Speciality</VCardTitle>
        <VCardText>
          Are you sure you want to delete <strong>{{ pendingDelete?.name }}</strong>?
          This cannot be undone.
        </VCardText>
        <VCardActions class="pa-5 pt-0">
          <VSpacer />
          <VBtn variant="text" @click="deleteDialog = false">Cancel</VBtn>
          <VBtn color="error" variant="elevated" rounded="lg" :loading="deleting" @click="handleDelete">
            Delete
          </VBtn>
        </VCardActions>
      </VCard>
    </VDialog>
  </VContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useSpecialityStore } from '../stores/specialityStore'
import type { Speciality } from '../stores/specialityStore'
import { useToastStore } from '../stores/toastStore'

const specialityStore = useSpecialityStore()
const toastStore = useToastStore()

const loading = ref(true)
const adding = ref(false)
const saving = ref(false)
const deleting = ref(false)

const newName = ref('')
const addError = ref('')

const editingId = ref<number | null>(null)
const editName = ref('')
const editError = ref('')

const deleteDialog = ref(false)
const pendingDelete = ref<Speciality | null>(null)

const headers = [
  { title: 'Name', key: 'name', sortable: true },
  { title: '', key: 'actions', align: 'end' as const, sortable: false }
]

onMounted(async () => {
  try {
    await specialityStore.fetchSpecialities()
  } catch {
    toastStore.show('Failed to load specialities.', 'error')
  } finally {
    loading.value = false
  }
})

async function handleAdd() {
  addError.value = ''
  if (!newName.value.trim()) {
    addError.value = 'Name is required.'
    return
  }
  adding.value = true
  try {
    await specialityStore.addSpeciality(newName.value.trim())
    toastStore.show('Speciality added.', 'success')
    newName.value = ''
  } catch (err: unknown) {
    toastStore.showError(err)
  } finally {
    adding.value = false
  }
}

function startEdit(item: Speciality) {
  editingId.value = item.id
  editName.value = item.name
  editError.value = ''
}

function cancelEdit() {
  editingId.value = null
  editName.value = ''
  editError.value = ''
}

async function handleSave(id: number) {
  editError.value = ''
  if (!editName.value.trim()) {
    editError.value = 'Name is required.'
    return
  }
  saving.value = true
  try {
    await specialityStore.updateSpeciality(id, editName.value.trim())
    toastStore.show('Speciality updated.', 'success')
    cancelEdit()
  } catch (err: unknown) {
    toastStore.showError(err)
  } finally {
    saving.value = false
  }
}

function confirmDelete(item: Speciality) {
  pendingDelete.value = item
  deleteDialog.value = true
}

async function handleDelete() {
  if (!pendingDelete.value) return
  deleting.value = true
  try {
    await specialityStore.deleteSpeciality(pendingDelete.value.id)
    toastStore.show('Speciality deleted.', 'success')
    deleteDialog.value = false
    pendingDelete.value = null
  } catch (err: unknown) {
    toastStore.showError(err)
  } finally {
    deleting.value = false
  }
}
</script>
