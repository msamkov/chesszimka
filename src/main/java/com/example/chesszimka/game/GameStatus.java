package com.example.chesszimka.game;

/** Текущее состояние сетевой партии. */
public enum GameStatus {
    /** Партия идёт, ходы принимаются. */
    IN_PROGRESS,
    /** Партия завершена (мат, пат, ничья или отключение соперника). */
    FINISHED
}

