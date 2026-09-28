import type { Chess, Move as ChessJsMove } from 'chess.js';

// Условная ценность фигур для эвристической оценки ходов (в пешках).
const PIECE_VALUE: Record<string, number> = {
  p: 1,
  n: 3,
  b: 3,
  r: 5,
  q: 9,
  k: 0,
};

/**
 * Очень простой эвристический «ИИ»: всегда делает легальный ход, немного
 * предпочитает взятия (взвешенные по ценности взятой фигуры) и шахи/маты,
 * со случайной составляющей, чтобы партии не выглядели полностью детерминированными.
 */
export function pickComputerMove(chess: Chess): ChessJsMove | null {
  const moves = chess.moves({ verbose: true }) as ChessJsMove[];
  if (moves.length === 0) {
    return null;
  }

  let best: ChessJsMove = moves[0];
  let bestScore = -Infinity;

  for (const move of moves) {
    let score = Math.random() * 2;
    if (move.captured) {
      score += (PIECE_VALUE[move.captured] ?? 0) * 10;
    }
    if (move.san.includes('#')) {
      score += 1000;
    } else if (move.san.includes('+')) {
      score += 4;
    }
    if (score > bestScore) {
      bestScore = score;
      best = move;
    }
  }
  return best;
}

/** Возвращает случайную задержку (мс) перед ходом компьютера, чтобы ход не выглядел мгновенным. */
export function randomComputerDelayMs(): number {
  return 500 + Math.floor(Math.random() * 900);
}
