import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

// Примечание: StrictMode намеренно не используется. Его двойной вызов
// эффектов в dev-режиме открывал бы и сразу закрывал одноразовое
// WebSocket-соединение для каждой игровой комнаты, что backend (справедливо)
// воспринимает как реальное отключение игрока и завершает партию.
createRoot(document.getElementById('root')!).render(<App />)
