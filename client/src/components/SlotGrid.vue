<template>
  <VRow dense>
    <VCol
      v-for="s in slots"
      :key="s.startTime"
      cols="6"
      sm="4"
      md="3"
    >
      <VBtn
        block
        rounded="lg"
        :color="slotColor(s.state)"
        :variant="s.state === 'AVAILABLE' ? 'tonal' : 'outlined'"
        :disabled="s.state !== 'AVAILABLE'"
        class="slot-btn py-3"
        height="auto"
        @click="s.state === 'AVAILABLE' && emit('select', s.startTime)"
      >
        <div class="d-flex flex-column">
          <span class="text-body-2 font-weight-bold">{{ s.startTime }}</span>
          <span class="text-caption" style="opacity:0.75">{{ s.state }}</span>
        </div>
      </VBtn>
    </VCol>
    <VCol v-if="slots.length === 0" cols="12">
      <VAlert type="info" variant="tonal" rounded="xl">
        No slots available for this date.
      </VAlert>
    </VCol>
  </VRow>
</template>

<script setup lang="ts">
import type { SlotDisplay } from '../stores/appointmentStore'

defineProps<{ slots: SlotDisplay[] }>()
const emit = defineEmits<{ select: [startTime: string] }>()

function slotColor(state: string): string {
  switch (state) {
    case 'AVAILABLE': return 'success'
    case 'LOCKED': return 'warning'
    case 'UNAVAILABLE': return 'error'
    default: return 'default'
  }
}
</script>
