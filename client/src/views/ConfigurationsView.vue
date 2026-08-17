<template>
  <VContainer class="py-6" max-width="700">
    <div class="text-h5 font-weight-bold mb-6">Configurations</div>
    <VCard>
      <VTable>
        <thead>
          <tr>
            <th class="text-left">Functionality</th>
            <th class="text-left">Enabled</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>Pinned sidebar</td>
            <td>
              <VSwitch
                :model-value="preferencesStore.pinnedSidebar"
                inset
                hide-details
                color="primary"
                @update:model-value="onTogglePinnedSidebar"
              />
            </td>
          </tr>
        </tbody>
      </VTable>
    </VCard>
  </VContainer>
</template>

<script setup lang="ts">
import { usePreferencesStore } from '../stores/preferencesStore'

const preferencesStore = usePreferencesStore()

function onTogglePinnedSidebar(value: boolean | null) {
  preferencesStore.updatePreferences({ pinnedSidebar: value ?? false })
}
</script>
