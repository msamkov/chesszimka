import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Chess } from 'chess.js';
import type { Square } from 'chess.js';
import { Chessboard } from 'react-chessboard';
import type { PieceDropHandlerArgs } from 'react-chessboard';
import { useNetwork } from '../context/NetworkContext';
import { GameInfoPanel } from '../components/GameInfoPanel';
import { PromotionDialog } from '../components/PromotionDialog';

/** Проверяет, является ли перетаскивание фигуры попыткой превращения пешки (без применения хода). */
function isPromotionAttempt(fen: string, from: string, to: string): boolean {
  try {
    const temp = new Chess(fen);
    const piece = temp.get(from as Square);
    if (!piece || piece.type !== 'p') {
      return false;
    }
    return (piece.color === 'w' && to[1] === '8') || (piece.color === 'b' && to[1] === '1');
  } catch {
    return false;
  }
}

/** Страница сетевой партии: доска отображает состояние, полученное от сервера, ходы отправляются как намерения. */
export function NetworkGamePage() {
  const { gameId = '' } = useParams();
  const navigate = useNavigate();
  const { activeGame, error, sendMove, clearActiveGame } = useNetwork();
  const [pendingPromotion, setPendingPromotion] = useState<{ from: Square; to: Square } | null>(null);

  // Подстраховка: если подходящее состояние партии не приходит в течение короткого времени
  // (например, устаревшая ссылка после перезапуска сервера), возвращаем пользователя в lobby
  // вместо показа «мёртвого» экрана.
  useEffect(() => {
    if (activeGame && activeGame.gameId === gameId) {
      return;
    }
    const timeout = window.setTimeout(() => {
      navigate('/network');
    }, 8000);
    return () => window.clearTimeout(timeout);
  }, [activeGame, gameId, navigate]);

  const state = useMemo(
    () => (activeGame && activeGame.gameId === gameId ? activeGame : null),
    [activeGame, gameId],
  );

  // Обрабатывает перетаскивание фигуры: локально проверяет только очевидные условия (ход/статус),
  // окончательную легальность хода проверяет сервер.
  function onPieceDrop({ sourceSquare, targetSquare }: PieceDropHandlerArgs): boolean {
    if (!state || targetSquare === null) {
      return false;
    }
    if (state.status !== 'IN_PROGRESS' || state.turn !== state.yourColor) {
      return false;
    }
    if (isPromotionAttempt(state.fen, sourceSquare, targetSquare)) {
      setPendingPromotion({ from: sourceSquare as Square, to: targetSquare as Square });
      return true;
    }
    sendMove(sourceSquare, targetSquare);
    return true;
  }

  // Сбрасывает текущую партию и возвращает пользователя в lobby (по кнопке «Новая игра»).
  function goToLobby() {
    clearActiveGame();
    navigate('/network');
  }

  if (!state) {
    return (
      <div className="card">
        <h2>Загрузка партии…</h2>
        {error && <div className="error-text">{error}</div>}
      </div>
    );
  }

  const statusText =
    state.status === 'FINISHED'
      ? 'Партия окончена'
      : state.turn === state.yourColor
        ? 'Ваш ход'
        : 'Ход соперника';

  return (
    <div className="game-layout">
      <GameInfoPanel
        whiteName={state.whiteName ?? '—'}
        blackName={state.blackName ?? '—'}
        yourColor={state.yourColor}
        turn={state.turn}
        inCheck={state.inCheck}
        statusText={statusText}
        resultMessage={state.status === 'FINISHED' ? state.resultMessage : null}
        onNewGame={goToLobby}
      />
      <div className="board-wrapper">
        <Chessboard
          options={{
            position: state.fen,
            boardOrientation: state.yourColor === 'WHITE' ? 'white' : 'black',
            onPieceDrop,
            allowDragging: state.status === 'IN_PROGRESS' && state.turn === state.yourColor,
            squareStyles:
              state.lastMoveFrom && state.lastMoveTo
                ? {
                    [state.lastMoveFrom]: { backgroundColor: 'rgba(79, 140, 255, 0.35)' },
                    [state.lastMoveTo]: { backgroundColor: 'rgba(79, 140, 255, 0.35)' },
                  }
                : undefined,
          }}
        />
      </div>
      {error && <div className="error-text">{error}</div>}
      {pendingPromotion && (
        <PromotionDialog
          color={state.yourColor === 'WHITE' ? 'w' : 'b'}
          onSelect={(pieceCode) => {
            const { from, to } = pendingPromotion;
            setPendingPromotion(null);
            sendMove(from, to, pieceCode);
          }}
        />
      )}
      <button className="link" onClick={() => navigate('/')}>
        ← На главную
      </button>
    </div>
  );
}
