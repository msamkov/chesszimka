package com.example.chesszimka.model;

/** Тип шахматной фигуры. */
public enum PieceType {
    PAWN('p'),
    KNIGHT('n'),
    BISHOP('b'),
    ROOK('r'),
    QUEEN('q'),
    KING('k');

    private final char fenChar;

    PieceType(char fenChar) {
        this.fenChar = fenChar;
    }

    /** Возвращает символ типа фигуры в нотации FEN (строчный). */
    public char fenChar() {
        return fenChar;
    }

    /** Определяет тип фигуры по символу FEN (регистр не важен). */
    public static PieceType fromFenChar(char c) {
        char lower = Character.toLowerCase(c);
        for (PieceType t : values()) {
            if (t.fenChar == lower) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown piece char: " + c);
    }

    /** Преобразует код превращения пешки ("q", "r", "b", "n") в тип фигуры, по умолчанию — ферзь. */
    public static PieceType fromPromotionCode(String code) {
        if (code == null) {
            return QUEEN;
        }
        return switch (code.toLowerCase()) {
            case "q" -> QUEEN;
            case "r" -> ROOK;
            case "b" -> BISHOP;
            case "n" -> KNIGHT;
            default -> QUEEN;
        };
    }
}
