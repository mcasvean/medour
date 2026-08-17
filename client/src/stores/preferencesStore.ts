import { defineStore } from 'pinia'
import api from '../api/index'
import { useToastStore } from './toastStore'

interface PreferencesState {
  pinnedSidebar: boolean
}

export const usePreferencesStore = defineStore('preferences', {
  state: (): PreferencesState => ({
    pinnedSidebar: false
  }),
  actions: {
    async fetchPreferences() {
      try {
        const res = await api.get<PreferencesState>('/users/me/preferences')
        this.pinnedSidebar = res.data.pinnedSidebar
      } catch {
        // silently ignore — default state is fine
      }
    },
    async updatePreferences(patch: Partial<PreferencesState>) {
      const previous = { ...this.$state }
      this.$patch(patch)
      try {
        const res = await api.patch<PreferencesState>('/users/me/preferences', patch)
        this.$patch({ pinnedSidebar: res.data.pinnedSidebar })
        useToastStore().show('Preferences saved', 'success')
      } catch (err) {
        this.$patch(previous)
        useToastStore().showError(err)
      }
    }
  }
})
