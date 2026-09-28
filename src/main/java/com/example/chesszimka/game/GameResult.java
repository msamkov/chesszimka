package com.example.chesszimka.game;

/** Итог завершившейся партии. */
public enum GameResult {
    /** Партия ещё не завершена. */
    NONE,
    /** Мат. */
    CHECKMATE,
    /** Пат — ничья. */
    STALEMATE,
    /** Ничья по правилу 50 ходов без взятий и продвижений пешек. */
    DRAW_50_MOVES,
    /** Ничья по троекратному повторению позиции. */
    DRAW_REPETITION,
    /** Ничья из-за недостаточного материала для мата. */
    DRAW_MATERIAL,
    /** Соперник отключился, партия завершена досрочно. */
    OPPONENT_DISCONNECTED
}

