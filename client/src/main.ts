import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './router/index'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'

const vuetify = createVuetify({
  components,
  directives,
  theme: {
    defaultTheme: 'light',
    themes: {
      light: {
        colors: {
          primary: '#1565C0',
          secondary: '#00897B',
          error: '#C62828',
          warning: '#EF6C00',
          success: '#2E7D32',
          info: '#0277BD',
        },
      },
    },
  },
  icons: { defaultSet: 'mdi' },
})

createApp(App).use(createPinia()).use(router).use(vuetify).mount('#app')
