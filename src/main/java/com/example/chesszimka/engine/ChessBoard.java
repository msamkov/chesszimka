package com.example.chesszimka.engine;

import com.example.chesszimka.model.Color;
import com.example.chesszimka.model.Move;
import com.example.chesszimka.model.Piece;
import com.example.chesszimka.model.PieceType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Полноценный шахматный движок: генерация легальных ходов, определение шаха,
 * мата и пата, рокировка, взятие на проходе, превращение пешки и условия ничьей.
 * Доска представлена так, что row 0 = горизонталь 1, col 0 = вертикаль 'a'.
 */
public class ChessBoard {

    private final Piece[][] squares = new Piece[8][8];
    private Color sideToMove = Color.WHITE;

    private boolean whiteKingSideCastle = true;
    private boolean whiteQueenSideCastle = true;
    private boolean blackKingSideCastle = true;
    private boolean blackQueenSideCastle = true;

    private Integer enPassantCol = null; // столбец, доступный для взятия на проходе в этом ходу
    private Integer enPassantRow = null;

    private int halfMoveClock = 0;
    private int fullMoveNumber = 1;

    private final Map<String, Integer> positionCounts = new HashMap<>();

    private Move lastMove;
    private String lastResultReason; // CHECKMATE / STALEMATE / DRAW_50 / DRAW_REPETITION / DRAW_MATERIAL

    /** Создаёт доску с начальной расстановкой фигур. */
    public ChessBoard() {
        setupInitialPosition();
        recordPosition();
    }

    /** Расставляет фигуры в стандартную начальную позицию. */
    private void setupInitialPosition() {
        PieceType[] backRank = {PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK};
        for (int col = 0; col < 8; col++) {
            squares[0][col] = new Piece(backRank[col], Color.WHITE);
            squares[1][col] = new Piece(PieceType.PAWN, Color.WHITE);
            squares[6][col] = new Piece(PieceType.PAWN, Color.BLACK);
            squares[7][col] = new Piece(backRank[col], Color.BLACK);
        }
    }

    /** Возвращает цвет стороны, которая должна ходить сейчас. */
    public Color getSideToMove() {
        return sideToMove;
    }

    /** Возвращает последний сделанный ход. */
    public Move getLastMove() {
        return lastMove;
    }

    /** Проверяет, находится ли король указанного цвета под шахом. */
    public boolean isInCheck(Color color) {
        int[] king = findKing(color);
        return king != null && isSquareAttacked(king[0], king[1], color.opposite());
    }

    /** Проверяет, является ли текущая позиция матом для стороны, чей сейчас ход. */
    public boolean isCheckmate() {
        return isInCheck(sideToMove) && legalMoves(sideToMove).isEmpty();
    }

    /** Проверяет, является ли текущая позиция патом для стороны, чей сейчас ход. */
    public boolean isStalemate() {
        return !isInCheck(sideToMove) && legalMoves(sideToMove).isEmpty();
    }

    /** Проверяет правило 50 ходов (ничья без взятий и ходов пешками). */
    public boolean isFiftyMoveDraw() {
        return halfMoveClock >= 100;
    }

    /** Проверяет троекратное повторение позиции. */
    public boolean isThreefoldRepetition() {
        String key = positionKey();
        return positionCounts.getOrDefault(key, 0) >= 3;
    }

    /** Проверяет недостаточность материала для мата (например, король против короля). */
    public boolean isInsufficientMaterial() {
        List<Piece> pieces = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (squares[r][c] != null) {
                    pieces.add(squares[r][c]);
                }
            }
        }
        if (pieces.size() <= 2) {
            return true; // король против короля
        }
        if (pieces.size() == 3) {
            for (Piece p : pieces) {
                if (p.type() == PieceType.KNIGHT || p.type() == PieceType.BISHOP) {
                    return true; // король+лёгкая фигура против короля
                }
            }
        }
        return false;
    }

    /** Возвращает причину окончания партии, определённую после последнего хода. */
    public String getLastResultReason() {
        return lastResultReason;
    }

    /** Возвращает все полностью легальные ходы для указанного цвета (с учётом безопасности короля). */
    public List<Move> legalMoves(Color color) {
        List<Move> pseudo = pseudoLegalMoves(color);
        List<Move> legal = new ArrayList<>();
        for (Move m : pseudo) {
            ChessBoard copy = copy();
            if (copy.applyMoveInternal(m)) {
                if (!copy.isInCheck(color)) {
                    legal.add(m);
                }
            }
        }
        return legal;
    }

    /** Ищет легальный ход, соответствующий from/to/превращению, применяет его и обновляет состояние партии. Возвращает null, если ход нелегален. */
    public Move tryMove(int fromRow, int fromCol, int toRow, int toCol, PieceType promotion) {
        Piece moving = squares[fromRow][fromCol];
        if (moving == null || moving.color() != sideToMove) {
            return null;
        }
        List<Move> legal = legalMoves(sideToMove);
        for (Move m : legal) {
            if (m.fromRow == fromRow && m.fromCol == fromCol && m.toRow == toRow && m.toCol == toCol) {
                if (m.promotion != null) {
                    PieceType wanted = promotion != null ? promotion : PieceType.QUEEN;
                    if (m.promotion != wanted) {
                        continue;
                    }
                }
                applyMoveInternal(m);
                lastMove = m;
                recordPosition();
                updateResultReason();
                return m;
            }
        }
        return null;
    }

    /** Пересчитывает причину окончания партии (мат, пат, разные виды ничьи) после текущего хода. */
    private void updateResultReason() {
        lastResultReason = null;
        if (isCheckmate()) {
            lastResultReason = "CHECKMATE";
        } else if (isStalemate()) {
            lastResultReason = "STALEMATE";
        } else if (isFiftyMoveDraw()) {
            lastResultReason = "DRAW_50";
        } else if (isThreefoldRepetition()) {
            lastResultReason = "DRAW_REPETITION";
        } else if (isInsufficientMaterial()) {
            lastResultReason = "DRAW_MATERIAL";
        }
    }

    /** Проверяет, закончена ли партия (обновляет и возвращает причину окончания). */
    public boolean isGameOver() {
        updateResultReason();
        return lastResultReason != null;
    }

    // --- генерация ходов ---

    /** Генерирует псевдолегальные ходы для цвета (без проверки, не остаётся ли король под шахом). */
    private List<Move> pseudoLegalMoves(Color color) {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = squares[r][c];
                if (p == null || p.color() != color) {
                    continue;
                }
                switch (p.type()) {
                    case PAWN -> addPawnMoves(moves, r, c, color);
                    case KNIGHT -> addKnightMoves(moves, r, c, color);
                    case BISHOP -> addSlidingMoves(moves, r, c, color, new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}});
                    case ROOK -> addSlidingMoves(moves, r, c, color, new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}});
                    case QUEEN -> addSlidingMoves(moves, r, c, color, new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1},
                            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}});
                    case KING -> addKingMoves(moves, r, c, color);
                }
            }
        }
        return moves;
    }

    /** Добавляет ходы пешки: одиночный/двойной шаг, взятие по диагонали, взятие на проходе, превращение. */
    private void addPawnMoves(List<Move> moves, int r, int c, Color color) {
        int dir = color == Color.WHITE ? 1 : -1;
        int startRow = color == Color.WHITE ? 1 : 6;
        int promotionRow = color == Color.WHITE ? 7 : 0;

        int oneRow = r + dir;
        if (inBounds(oneRow, c) && squares[oneRow][c] == null) {
            addPawnMoveWithPromotion(moves, r, c, oneRow, c, promotionRow, false);
            int twoRow = r + 2 * dir;
            if (r == startRow && inBounds(twoRow, c) && squares[twoRow][c] == null) {
                Move m = new Move(r, c, twoRow, c, null);
                m.doublePawnPush = true;
                moves.add(m);
            }
        }
        for (int dc : new int[]{-1, 1}) {
            int nc = c + dc;
            if (!inBounds(oneRow, nc)) {
                continue;
            }
            Piece target = squares[oneRow][nc];
            if (target != null && target.color() != color) {
                addPawnMoveWithPromotion(moves, r, c, oneRow, nc, promotionRow, true);
            } else if (target == null && enPassantRow != null && enPassantRow == oneRow && enPassantCol == nc) {
                Move m = new Move(r, c, oneRow, nc, null);
                m.enPassant = true;
                m.capture = true;
                moves.add(m);
            }
        }
    }

    /** Добавляет ход пешки, разворачивая его в 4 варианта превращения, если пешка достигает последней горизонтали. */
    private void addPawnMoveWithPromotion(List<Move> moves, int r, int c, int toRow, int toCol, int promotionRow, boolean capture) {
        if (toRow == promotionRow) {
            for (PieceType promo : new PieceType[]{PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT}) {
                Move m = new Move(r, c, toRow, toCol, promo);
                m.capture = capture;
                moves.add(m);
            }
        } else {
            Move m = new Move(r, c, toRow, toCol, null);
            m.capture = capture;
            moves.add(m);
        }
    }

    /** Добавляет ходы коня (буквой «Г»). */
    private void addKnightMoves(List<Move> moves, int r, int c, Color color) {
        int[][] deltas = {{1, 2}, {2, 1}, {-1, 2}, {-2, 1}, {1, -2}, {2, -1}, {-1, -2}, {-2, -1}};
        for (int[] d : deltas) {
            int nr = r + d[0];
            int nc = c + d[1];
            if (!inBounds(nr, nc)) {
                continue;
            }
            Piece target = squares[nr][nc];
            if (target == null || target.color() != color) {
                Move m = new Move(r, c, nr, nc, null);
                m.capture = target != null;
                moves.add(m);
            }
        }
    }

    /** Добавляет ходы дальнобойных фигур (слон, ладья, ферзь) вдоль заданных направлений до препятствия. */
    private void addSlidingMoves(List<Move> moves, int r, int c, Color color, int[][] directions) {
        for (int[] d : directions) {
            int nr = r + d[0];
            int nc = c + d[1];
            while (inBounds(nr, nc)) {
                Piece target = squares[nr][nc];
                if (target == null) {
                    moves.add(new Move(r, c, nr, nc, null));
                } else {
                    if (target.color() != color) {
                        Move m = new Move(r, c, nr, nc, null);
                        m.capture = true;
                        moves.add(m);
                    }
                    break;
                }
                nr += d[0];
                nc += d[1];
            }
        }
    }

    /** Добавляет обычные ходы короля на соседние клетки, а также ходы рокировки, если она доступна. */
    private void addKingMoves(List<Move> moves, int r, int c, Color color) {
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) {
                    continue;
                }
                int nr = r + dr;
                int nc = c + dc;
                if (!inBounds(nr, nc)) {
                    continue;
                }
                Piece target = squares[nr][nc];
                if (target == null || target.color() != color) {
                    Move m = new Move(r, c, nr, nc, null);
                    m.capture = target != null;
                    moves.add(m);
                }
            }
        }
        // рокировка
        int homeRow = color == Color.WHITE ? 0 : 7;
        if (r == homeRow && c == 4 && !isSquareAttacked(r, c, color.opposite())) {
            boolean kingSideRight = color == Color.WHITE ? whiteKingSideCastle : blackKingSideCastle;
            boolean queenSideRight = color == Color.WHITE ? whiteQueenSideCastle : blackQueenSideCastle;
            if (kingSideRight && squares[homeRow][5] == null && squares[homeRow][6] == null
                    && squares[homeRow][7] != null && squares[homeRow][7].type() == PieceType.ROOK
                    && !isSquareAttacked(homeRow, 5, color.opposite())
                    && !isSquareAttacked(homeRow, 6, color.opposite())) {
                Move m = new Move(r, c, homeRow, 6, null);
                m.castleKingSide = true;
                moves.add(m);
            }
            if (queenSideRight && squares[homeRow][3] == null && squares[homeRow][2] == null && squares[homeRow][1] == null
                    && squares[homeRow][0] != null && squares[homeRow][0].type() == PieceType.ROOK
                    && !isSquareAttacked(homeRow, 3, color.opposite())
                    && !isSquareAttacked(homeRow, 2, color.opposite())) {
                Move m = new Move(r, c, homeRow, 2, null);
                m.castleQueenSide = true;
                moves.add(m);
            }
        }
    }

    /** Проверяет, атакована ли указанная клетка фигурами заданного цвета. */
    private boolean isSquareAttacked(int row, int col, Color byColor) {
        // атаки пешками
        int pawnDir = byColor == Color.WHITE ? -1 : 1; // клетки атакуются с точки зрения атакующего
        for (int dc : new int[]{-1, 1}) {
            int pr = row + pawnDir;
            int pc = col + dc;
            if (inBounds(pr, pc)) {
                Piece p = squares[pr][pc];
                if (p != null && p.color() == byColor && p.type() == PieceType.PAWN) {
                    return true;
                }
            }
        }
        // атаки конём
        int[][] knightDeltas = {{1, 2}, {2, 1}, {-1, 2}, {-2, 1}, {1, -2}, {2, -1}, {-1, -2}, {-2, -1}};
        for (int[] d : knightDeltas) {
            int nr = row + d[0];
            int nc = col + d[1];
            if (inBounds(nr, nc)) {
                Piece p = squares[nr][nc];
                if (p != null && p.color() == byColor && p.type() == PieceType.KNIGHT) {
                    return true;
                }
            }
        }
        // атаки королём
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = row + dr;
                int nc = col + dc;
                if (inBounds(nr, nc)) {
                    Piece p = squares[nr][nc];
                    if (p != null && p.color() == byColor && p.type() == PieceType.KING) {
                        return true;
                    }
                }
            }
        }
        // дальнобойные атаки: ладья/ферзь (по горизонтали/вертикали)
        int[][] straight = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : straight) {
            int nr = row + d[0];
            int nc = col + d[1];
            while (inBounds(nr, nc)) {
                Piece p = squares[nr][nc];
                if (p != null) {
                    if (p.color() == byColor && (p.type() == PieceType.ROOK || p.type() == PieceType.QUEEN)) {
                        return true;
                    }
                    break;
                }
                nr += d[0];
                nc += d[1];
            }
        }
        // дальнобойные атаки: слон/ферзь (по диагонали)
        int[][] diagonal = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] d : diagonal) {
            int nr = row + d[0];
            int nc = col + d[1];
            while (inBounds(nr, nc)) {
                Piece p = squares[nr][nc];
                if (p != null) {
                    if (p.color() == byColor && (p.type() == PieceType.BISHOP || p.type() == PieceType.QUEEN)) {
                        return true;
                    }
                    break;
                }
                nr += d[0];
                nc += d[1];
            }
        }
        return false;
    }

    /** Находит координаты короля указанного цвета на доске. */
    private int[] findKing(Color color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = squares[r][c];
                if (p != null && p.color() == color && p.type() == PieceType.KING) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    /** Проверяет, что координаты находятся в пределах доски 8x8. */
    private boolean inBounds(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }

    /** Применяет ход к этому экземпляру доски без дополнительных проверок легальности. Возвращает true, если ход применён. */
    private boolean applyMoveInternal(Move m) {
        Piece moving = squares[m.fromRow][m.fromCol];
        if (moving == null) {
            return false;
        }
        Color color = moving.color();
        boolean resetHalfMove = moving.type() == PieceType.PAWN || m.capture;

        // удаление фигуры, взятой на проходе
        if (m.enPassant) {
            int capturedRow = color == Color.WHITE ? m.toRow - 1 : m.toRow + 1;
            squares[capturedRow][m.toCol] = null;
        }

        // перемещение фигуры
        squares[m.toRow][m.toCol] = m.promotion != null ? new Piece(m.promotion, color) : moving;
        squares[m.fromRow][m.fromCol] = null;

        // перемещение ладьи при рокировке
        if (m.castleKingSide) {
            int homeRow = m.fromRow;
            squares[homeRow][5] = squares[homeRow][7];
            squares[homeRow][7] = null;
        } else if (m.castleQueenSide) {
            int homeRow = m.fromRow;
            squares[homeRow][3] = squares[homeRow][0];
            squares[homeRow][0] = null;
        }

        // обновление прав на рокировку
        if (moving.type() == PieceType.KING) {
            if (color == Color.WHITE) {
                whiteKingSideCastle = false;
                whiteQueenSideCastle = false;
            } else {
                blackKingSideCastle = false;
                blackQueenSideCastle = false;
            }
        }
        revokeCastlingIfRookMoved(m.fromRow, m.fromCol);
        revokeCastlingIfRookMoved(m.toRow, m.toCol);

        // обновление клетки, доступной для взятия на проходе
        if (m.doublePawnPush) {
            enPassantRow = (m.fromRow + m.toRow) / 2;
            enPassantCol = m.toCol;
        } else {
            enPassantRow = null;
            enPassantCol = null;
        }

        // счётчики полуходов / полных ходов
        if (resetHalfMove) {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }
        if (color == Color.BLACK) {
            fullMoveNumber++;
        }

        sideToMove = color.opposite();
        return true;
    }

    /** Отзывает право на рокировку с соответствующей стороны, если ладья на этой клетке сдвинулась или была взята. */
    private void revokeCastlingIfRookMoved(int row, int col) {
        if (row == 0 && col == 0) whiteQueenSideCastle = false;
        if (row == 0 && col == 7) whiteKingSideCastle = false;
        if (row == 7 && col == 0) blackQueenSideCastle = false;
        if (row == 7 && col == 7) blackKingSideCastle = false;
    }

    /** Учитывает текущую позицию в счётчике повторений (для правила троекратного повторения). */
    private void recordPosition() {
        String key = positionKey();
        positionCounts.merge(key, 1, Integer::sum);
    }

    /** Строит строковый ключ, однозначно описывающий текущую позицию (для сравнения повторений). */
    private String positionKey() {
        StringBuilder sb = new StringBuilder();
        for (int r = 7; r >= 0; r--) {
            for (int c = 0; c < 8; c++) {
                Piece p = squares[r][c];
                sb.append(p == null ? '.' : p.fenChar());
            }
        }
        sb.append(sideToMove == Color.WHITE ? 'w' : 'b');
        sb.append(whiteKingSideCastle ? 'K' : '-');
        sb.append(whiteQueenSideCastle ? 'Q' : '-');
        sb.append(blackKingSideCastle ? 'k' : '-');
        sb.append(blackQueenSideCastle ? 'q' : '-');
        sb.append(enPassantCol == null ? "-" : ("" + enPassantCol + enPassantRow));
        return sb.toString();
    }

    /** Создаёт полную независимую копию доски (используется для проверки легальности хода). */
    public ChessBoard copy() {
        ChessBoard b = new ChessBoard();
        for (int r = 0; r < 8; r++) {
            System.arraycopy(this.squares[r], 0, b.squares[r], 0, 8);
        }
        b.sideToMove = this.sideToMove;
        b.whiteKingSideCastle = this.whiteKingSideCastle;
        b.whiteQueenSideCastle = this.whiteQueenSideCastle;
        b.blackKingSideCastle = this.blackKingSideCastle;
        b.blackQueenSideCastle = this.blackQueenSideCastle;
        b.enPassantCol = this.enPassantCol;
        b.enPassantRow = this.enPassantRow;
        b.halfMoveClock = this.halfMoveClock;
        b.fullMoveNumber = this.fullMoveNumber;
        return b;
    }

    /** Сериализует текущую позицию в стандартную нотацию FEN. */
    public String toFen() {
        StringBuilder sb = new StringBuilder();
        for (int r = 7; r >= 0; r--) {
            int empty = 0;
            for (int c = 0; c < 8; c++) {
                Piece p = squares[r][c];
                if (p == null) {
                    empty++;
                } else {
                    if (empty > 0) {
                        sb.append(empty);
                        empty = 0;
                    }
                    sb.append(p.fenChar());
                }
            }
            if (empty > 0) {
                sb.append(empty);
            }
            if (r > 0) {
                sb.append('/');
            }
        }
        sb.append(' ').append(sideToMove == Color.WHITE ? 'w' : 'b').append(' ');
        String castling = "" + (whiteKingSideCastle ? "K" : "") + (whiteQueenSideCastle ? "Q" : "")
                + (blackKingSideCastle ? "k" : "") + (blackQueenSideCastle ? "q" : "");
        sb.append(castling.isEmpty() ? "-" : castling).append(' ');
        sb.append(enPassantCol == null ? "-" : Move.squareName(enPassantRow, enPassantCol)).append(' ');
        sb.append(halfMoveClock).append(' ').append(fullMoveNumber);
        return sb.toString();
    }
}
