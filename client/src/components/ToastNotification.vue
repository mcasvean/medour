<template>
  <div class="toast-container">
    <TransitionGroup name="toast">
      <div
        v-for="toast in toastStore.toasts"
        :key="toast.id"
        class="toast-card"
        :class="`toast-card--${toast.type}`"
      >
        <VIcon :icon="iconByType[toast.type]" :color="colorByType[toast.type]" size="20" />
        <span class="toast-message">{{ toast.message }}</span>
        <VBtn
          icon="mdi-close"
          size="x-small"
          variant="text"
          :color="colorByType[toast.type]"
          @click="toastStore.dismiss(toast.id)"
        />
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup lang="ts">
import { useToastStore } from '@/stores/toastStore'

const toastStore = useToastStore()

const iconByType = {
  error: 'mdi-alert-circle',
  success: 'mdi-check-circle',
  warning: 'mdi-alert'
} as const

const colorByType = {
  error: '#EF5350',
  success: '#4CAF50',
  warning: '#FB8C00'
} as const
</script>

<style scoped>
.toast-container {
  position: fixed;
  bottom: 24px;
  right: 24px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 9999;
  pointer-events: none;
}

.toast-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.18);
  min-width: 260px;
  max-width: 400px;
  pointer-events: all;
}

.toast-card--error  { border-left: 4px solid #EF5350; }
.toast-card--success { border-left: 4px solid #4CAF50; }
.toast-card--warning { border-left: 4px solid #FB8C00; }

.toast-message {
  flex: 1;
  font-size: 0.9rem;
  color: #222;
  word-break: break-word;
}

/* TransitionGroup animations */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.28s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateY(16px);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(40px);
}
</style>
