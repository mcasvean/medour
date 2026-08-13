<template>
  <ul class="slot-grid">
    <li
      v-for="s in slots"
      :key="s.startTime"
      :class="{
        available: s.state === 'AVAILABLE',
        locked: s.state === 'LOCKED',
        unavailable: s.state === 'UNAVAILABLE',
      }"
      class="slot-cell"
    >
      {{ s.startTime }} – {{ s.endTime }}
      <span class="slot-state">{{ s.state }}</span>
    </li>
  </ul>
</template>

<script setup lang="ts">
import type { SlotDisplay } from '../stores/appointmentStore'

defineProps<{ slots: SlotDisplay[] }>()
</script>

<style scoped>
.slot-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.5rem;
  list-style: none;
  padding: 0;
  margin: 1rem 0;
}

.slot-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.5rem;
  border-radius: 4px;
  font-size: 0.85rem;
  font-weight: 500;
}

.available {
  background: #d4edda;
  color: #155724;
}

.locked {
  background: #fff3cd;
  color: #856404;
}

.unavailable {
  background: #f8d7da;
  color: #721c24;
}

.slot-state {
  font-size: 0.7rem;
  font-weight: 400;
  opacity: 0.75;
}
</style>
