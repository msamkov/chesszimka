import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { HomePage } from './pages/HomePage';
import { ComputerGamePage } from './pages/ComputerGamePage';
import { NetworkLobbyPage } from './pages/NetworkLobbyPage';
import { NetworkGamePage } from './pages/NetworkGamePage';
import { NetworkProvider } from './context/NetworkContext';
import { InvitationOverlay } from './components/InvitationOverlay';

/** Корневой компонент приложения: маршрутизация между экранами и общий провайдер сетевого контекста. */
function App() {
  return (
    <BrowserRouter>
      <NetworkProvider>
        <div className="app-shell">
          <h1 className="app-title">♟ Шахматы</h1>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/computer" element={<ComputerGamePage />} />
            <Route path="/network" element={<NetworkLobbyPage />} />
            <Route path="/game/:gameId" element={<NetworkGamePage />} />
          </Routes>
        </div>
        <InvitationOverlay />
      </NetworkProvider>
    </BrowserRouter>
  );
}

export default App;
