import { useCallback, useMemo, useRef, useState } from 'react';
import { Chess } from 'chess.js';
import type { Square } from 'chess.js';
import { Chessboard } from 'react-chessboard';
import type { PieceDropHandlerArgs } from 'react-chessboard';
import { useNavigate } from 'react-router-dom';
import { getStoredName } from '../lib/names';
import { pickComputerMove, randomComputerDelayMs } from '../lib/computerAi';
import { GameInfoPanel } from '../components/GameInfoPanel';
import { PromotionDialog } from '../components/PromotionDialog';

// Вариант выбора цвета игроком перед началом партии против компьютера.
type PlayerColorChoice = 'white' | 'black' | 'random';

/** Преобразует выбор пользователя ('white'/'black'/'random') в конкретный цвет фигур. */
function resolveColor(choice: PlayerColorChoice): 'w' | 'b' {
  if (choice === 'random') {
    return Math.random() < 0.5 ? 'w' : 'b';
  }
  return choice === 'white' ? 'w' : 'b';
}

/** Формирует человекочитаемое сообщение об итоге партии против компьютера, либо null, если партия не окончена. */
function buildResultMessage(chess: Chess, playerName: string, playerColor: 'w' | 'b'): string | null {
  if (!chess.isGameOver()) {
    return null;
  }
  if (chess.isCheckmate()) {
    const winnerIsPlayer = chess.turn() !== playerColor; // сторона, чей сейчас ход, и есть та, кому поставили мат
    const winnerName = winnerIsPlayer ? playerName : 'Компьютер';
    return `Мат. Победил(а) ${winnerName}.`;
  }
  if (chess.isStalemate()) {
    return 'Пат. Ничья.';
  }
  if (chess.isInsufficientMaterial()) {
    return 'Ничья (недостаточно материала для мата).';
  }
  if (chess.isThreefoldRepetition()) {
    return 'Ничья (троекратное повторение позиции).';
  }
  if (chess.isDrawByFiftyMoves()) {
    return 'Ничья (правило 50 ходов).';
  }
  if (chess.isDraw()) {
    return 'Ничья.';
  }
  return null;
}

/** Страница игры против компьютера: выбор цвета, доска и локальный (клиентский) шахматный движок chess.js. */
export function ComputerGamePage() {
  const navigate = useNavigate();
  const playerName = useMemo(() => getStoredName() || 'Игрок', []);
  const [colorChoice, setColorChoice] = useState<PlayerColorChoice>('white');
  const [started, setStarted] = useState(false);
  const [playerColor, setPlayerColor] = useState<'w' | 'b'>('w');
  const gameRef = useRef(new Chess());
  const [fen, setFen] = useState(gameRef.current.fen());
  const [pendingPromotion, setPendingPromotion] = useState<{ from: Square; to: Square } | null>(null);
  const [thinking, setThinking] = useState(false);

  const game = gameRef.current;
  const resultMessage = started ? buildResultMessage(game, playerName, playerColor) : null;
  const isGameOver = game.isGameOver();

  // Планирует ход компьютера с небольшой случайной задержкой, чтобы он не выглядел мгновенным.
  const scheduleComputerMove = useCallback(() => {
    setThinking(true);
    window.setTimeout(() => {
      const move = pickComputerMove(gameRef.current);
      if (move) {
        gameRef.current.move({ from: move.from, to: move.to, promotion: move.promotion });
        setFen(gameRef.current.fen());
      }
      setThinking(false);
    }, randomComputerDelayMs());
  }, []);

  // Начинает новую партию с выбранным цветом; если игрок играет чёрными, первым ходит компьютер.
  function startGame() {
    const color = resolveColor(colorChoice);
    gameRef.current = new Chess();
    setPlayerColor(color);
    setFen(gameRef.current.fen());
    setStarted(true);
    setPendingPromotion(null);
    if (color === 'b') {
      scheduleComputerMove();
    }
  }

  // Сбрасывает состояние партии и возвращает экран выбора цвета.
  function newGame() {
    setStarted(false);
    setThinking(false);
    setPendingPromotion(null);
    gameRef.current = new Chess();
    setFen(gameRef.current.fen());
  }

  // Применяет ход игрока к локальной доске и, если партия продолжается, планирует ответ компьютера.
  function completeMove(from: Square, to: Square, promotion?: 'q' | 'r' | 'b' | 'n') {
    try {
      const move = gameRef.current.move({ from, to, promotion: promotion ?? 'q' });
      if (!move) {
        return false;
      }
    } catch {
      return false;
    }
    setFen(gameRef.current.fen());
    if (!gameRef.current.isGameOver()) {
      scheduleComputerMove();
    }
    return true;
  }

  // Обрабатывает перетаскивание фигуры игроком на доске (с учётом превращения пешки).
  function onPieceDrop({ sourceSquare, targetSquare }: PieceDropHandlerArgs): boolean {
    if (!started || isGameOver || thinking || !targetSquare) {
      return false;
    }
    if (game.turn() !== playerColor) {
      return false;
    }
    const piece = game.get(sourceSquare as Square);
    const isPromotion =
      piece?.type === 'p' &&
      ((piece.color === 'w' && targetSquare[1] === '8') || (piece.color === 'b' && targetSquare[1] === '1'));

    if (isPromotion) {
      setPendingPromotion({ from: sourceSquare as Square, to: targetSquare as Square });
      return true; // оптимистично принимаем; реальный ход применяется после выбора фигуры
    }
    return completeMove(sourceSquare as Square, targetSquare as Square);
  }

  const statusText = !started
    ? 'Выберите цвет и начните партию'
    : isGameOver
      ? 'Партия окончена'
      : thinking
        ? 'Компьютер думает…'
        : game.turn() === playerColor
          ? 'Ваш ход'
          : 'Ход компьютера';

  if (!started) {
    return (
      <div className="card">
        <h2>Игра с компьютером</h2>
        <p className="hint">Выберите, за кого будете играть.</p>
        <div className="color-choice">
          <button
            className={colorChoice === 'white' ? 'selected' : ''}
            onClick={() => setColorChoice('white')}
          >
            Белые
          </button>
          <button
            className={colorChoice === 'black' ? 'selected' : ''}
            onClick={() => setColorChoice('black')}
          >
            Чёрные
          </button>
          <button
            className={colorChoice === 'random' ? 'selected' : ''}
            onClick={() => setColorChoice('random')}
          >
            Случайно
          </button>
        </div>
        <button className="primary" onClick={startGame}>
          Начать партию
        </button>
        <button className="link" onClick={() => navigate('/')}>
          ← На главную
        </button>
      </div>
    );
  }

  return (
    <div className="game-layout">
      <GameInfoPanel
        whiteName={playerColor === 'w' ? playerName : 'Компьютер'}
        blackName={playerColor === 'b' ? playerName : 'Компьютер'}
        yourColor={playerColor === 'w' ? 'WHITE' : 'BLACK'}
        turn={game.turn() === 'w' ? 'WHITE' : 'BLACK'}
        inCheck={!isGameOver && game.isCheck()}
        statusText={statusText}
        resultMessage={resultMessage}
        onNewGame={newGame}
      />
      <div className="board-wrapper">
        <Chessboard
          options={{
            position: fen,
            boardOrientation: playerColor === 'w' ? 'white' : 'black',
            onPieceDrop,
            allowDragging: !isGameOver && !thinking && game.turn() === playerColor,
          }}
        />
      </div>
      {pendingPromotion && (
        <PromotionDialog
          color={playerColor}
          onSelect={(pieceCode) => {
            const { from, to } = pendingPromotion;
            setPendingPromotion(null);
            completeMove(from, to, pieceCode);
          }}
        />
      )}
      <button className="link" onClick={() => navigate('/')}>
        ← На главную
      </button>
    </div>
  );
}
