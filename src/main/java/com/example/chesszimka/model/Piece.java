package com.example.chesszimka.model;

/** Шахматная фигура: тип фигуры и её цвет. */
public record Piece(PieceType type, Color color) {

    /** Возвращает символ фигуры в нотации FEN (заглавный для белых, строчный для чёрных). */
    public char fenChar() {
        char c = type.fenChar();
        return color == Color.WHITE ? Character.toUpperCase(c) : c;
    }
}

