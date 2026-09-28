package com.example.chesszimka.websocket.dto;

/** Полный достоверный снимок состояния партии, отправляемый обоим участникам после каждого изменения. */
public class StateMessage {
    public final String type = "GAME_STATE";
    private String gameId;
    private String fen;
    private String turn;
    private String status;
    private String result;
    private String resultMessage;
    private String whiteName;
    private String blackName;
    private String yourColor;
    private boolean inCheck;
    private String lastMoveFrom;
    private String lastMoveTo;

    /** Возвращает тип сообщения ("GAME_STATE"). */
    public String getType() {
        return type;
    }

    /** Возвращает id партии. */
    public String getGameId() {
        return gameId;
    }

    /** Устанавливает id партии. */
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    /** Возвращает текущую позицию в нотации FEN. */
    public String getFen() {
        return fen;
    }

    /** Устанавливает позицию в нотации FEN. */
    public void setFen(String fen) {
        this.fen = fen;
    }

    /** Возвращает цвет стороны, чей сейчас ход. */
    public String getTurn() {
        return turn;
    }

    /** Устанавливает цвет стороны, чей сейчас ход. */
    public void setTurn(String turn) {
        this.turn = turn;
    }

    /** Возвращает статус партии (IN_PROGRESS/FINISHED). */
    public String getStatus() {
        return status;
    }

    /** Устанавливает статус партии. */
    public void setStatus(String status) {
        this.status = status;
    }

    /** Возвращает результат партии (если завершена). */
    public String getResult() {
        return result;
    }

    /** Устанавливает результат партии. */
    public void setResult(String result) {
        this.result = result;
    }

    /** Возвращает человекочитаемое сообщение об итоге партии. */
    public String getResultMessage() {
        return resultMessage;
    }

    /** Устанавливает человекочитаемое сообщение об итоге партии. */
    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    /** Возвращает имя игрока за белых. */
    public String getWhiteName() {
        return whiteName;
    }

    /** Устанавливает имя игрока за белых. */
    public void setWhiteName(String whiteName) {
        this.whiteName = whiteName;
    }

    /** Возвращает имя игрока за чёрных. */
    public String getBlackName() {
        return blackName;
    }

    /** Устанавливает имя игрока за чёрных. */
    public void setBlackName(String blackName) {
        this.blackName = blackName;
    }

    /** Возвращает цвет, назначенный получателю этого сообщения. */
    public String getYourColor() {
        return yourColor;
    }

    /** Устанавливает цвет получателя. */
    public void setYourColor(String yourColor) {
        this.yourColor = yourColor;
    }

    /** Возвращает true, если сторона, чей сейчас ход, находится под шахом. */
    public boolean isInCheck() {
        return inCheck;
    }

    /** Устанавливает признак шаха. */
    public void setInCheck(boolean inCheck) {
        this.inCheck = inCheck;
    }

    /** Возвращает клетку начала последнего хода (для подсветки на доске). */
    public String getLastMoveFrom() {
        return lastMoveFrom;
    }

    /** Устанавливает клетку начала последнего хода. */
    public void setLastMoveFrom(String lastMoveFrom) {
        this.lastMoveFrom = lastMoveFrom;
    }

    /** Возвращает клетку окончания последнего хода (для подсветки на доске). */
    public String getLastMoveTo() {
        return lastMoveTo;
    }

    /** Устанавливает клетку окончания последнего хода. */
    public void setLastMoveTo(String lastMoveTo) {
        this.lastMoveTo = lastMoveTo;
    }
}
