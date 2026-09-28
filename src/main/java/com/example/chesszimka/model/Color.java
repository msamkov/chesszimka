package com.example.chesszimka.model;

/** Цвет фигур/стороны в шахматной партии. */
public enum Color {
    WHITE,
    BLACK;

    /** Возвращает противоположный цвет. */
    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}

