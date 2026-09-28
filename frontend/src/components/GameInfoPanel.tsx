// Параметры информационной панели партии (имена игроков, очередь хода, статус).
interface GameInfoPanelProps {
  whiteName: string;
  blackName: string;
  yourColor: 'WHITE' | 'BLACK';
  turn: 'WHITE' | 'BLACK';
  inCheck: boolean;
  statusText: string;
  resultMessage: string | null;
  roomCode?: string;
  onNewGame: () => void;
}

/** Панель сведений о партии: имена игроков, чей ход, статус/результат и кнопка новой игры. */
export function GameInfoPanel({
  whiteName,
  blackName,
  yourColor,
  turn,
  inCheck,
  statusText,
  resultMessage,
  roomCode,
  onNewGame,
}: GameInfoPanelProps) {
  return (
    <>
      <div className={`player-row ${turn === 'BLACK' ? 'active' : ''}`}>
        <span className="player-name">
          ⚫ {blackName} {yourColor === 'BLACK' ? '(вы)' : ''}
        </span>
        <span className="player-color">чёрные</span>
      </div>

      {roomCode && <div className="hint">Код комнаты: {roomCode}</div>}

      <div className={`status-bar ${inCheck ? 'check' : ''}`}>{statusText}</div>

      {resultMessage && <div className="result-banner">{resultMessage}</div>}

      <div className={`player-row ${turn === 'WHITE' ? 'active' : ''}`}>
        <span className="player-name">
          ⚪ {whiteName} {yourColor === 'WHITE' ? '(вы)' : ''}
        </span>
        <span className="player-color">белые</span>
      </div>

      <div className="footer-actions">
        <button className="primary" onClick={onNewGame}>
          Новая игра
        </button>
      </div>
    </>
  );
}
