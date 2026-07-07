import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import enUS from '@/locales/en-us.json'
import ptBR from '@/locales/pt-br.json'

void i18n.use(initReactI18next).init({
  resources: {
    'pt-BR': { translation: ptBR },
    'en-US': { translation: enUS },
  },
  lng: 'pt-BR',
  fallbackLng: 'en-US',
  interpolation: {
    escapeValue: false,
  },
})

export default i18n
