import { useNavigate } from 'react-router-dom';
import { useNetwork } from '../context/NetworkContext';

/** Экран lobby сетевой игры: список онлайн-игроков, приглашения и ожидание ответа. */
export function NetworkLobbyPage() {
  const navigate = useNavigate();
  const { connected, lobbyPlayers, outgoingInviteTargetId, notice, invite, cancelInvite } = useNetwork();

  const pendingTarget = lobbyPlayers.find((p) => p.id === outgoingInviteTargetId);

  return (
    <div className="card">
      <h2>Игроки онлайн</h2>

      {!connected && <p className="hint">Подключение к серверу…</p>}

      {connected && lobbyPlayers.length === 0 && (
        <p className="hint">Сейчас нет игроков, готовых начать игру.</p>
      )}

      {connected && lobbyPlayers.length > 0 && (
        <ul className="player-list">
          {lobbyPlayers.map((player) => (
            <li key={player.id} className="player-list-item">
              <span>🟢 {player.name}</span>
              <button
                className="primary"
                disabled={outgoingInviteTargetId !== null}
                onClick={() => invite(player.id)}
              >
                Играть
              </button>
            </li>
          ))}
        </ul>
      )}

      {outgoingInviteTargetId && (
        <div className="waiting-banner">
          <p>Ждём ответа{pendingTarget ? ` от ${pendingTarget.name}` : ''}…</p>
          <button className="link" onClick={cancelInvite}>
            Отменить приглашение
          </button>
        </div>
      )}

      {notice && <div className="hint">{notice.text}</div>}

      <button className="link" onClick={() => navigate('/')}>
        ← На главную
      </button>
    </div>
  );
}
