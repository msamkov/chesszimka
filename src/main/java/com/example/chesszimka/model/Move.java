package com.example.chesszimka.model;

/**
 * Один шахматный ход в 0-based координатах строка/столбец.
 * row 0 = горизонталь 1 (задний ряд белых), col 0 = вертикаль 'a'.
 */
public class Move {
    public final int fromRow;
    public final int fromCol;
    public final int toRow;
    public final int toCol;
    public final PieceType promotion; // null, если это не превращение пешки
    public boolean capture;
    public boolean enPassant;
    public boolean castleKingSide;
    public boolean castleQueenSide;
    public boolean doublePawnPush;

    /** Создаёт ход из одной клетки в другую с опциональным превращением пешки. */
    public Move(int fromRow, int fromCol, int toRow, int toCol, PieceType promotion) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.promotion = promotion;
    }

    /** Преобразует координаты в шахматную нотацию клетки (например, "e4"). */
    public static String squareName(int row, int col) {
        char file = (char) ('a' + col);
        int rank = row + 1;
        return "" + file + rank;
    }

    /** Возвращает номер строки (0-based) из строковой нотации клетки. */
    public static int rowFromSquare(String square) {
        return square.charAt(1) - '1';
    }

    /** Возвращает номер столбца (0-based) из строковой нотации клетки. */
    public static int colFromSquare(String square) {
        return square.charAt(0) - 'a';
    }

    /** Возвращает клетку начала хода в шахматной нотации. */
    public String fromSquare() {
        return squareName(fromRow, fromCol);
    }

    /** Возвращает клетку окончания хода в шахматной нотации. */
    public String toSquare() {
        return squareName(toRow, toCol);
    }
}
