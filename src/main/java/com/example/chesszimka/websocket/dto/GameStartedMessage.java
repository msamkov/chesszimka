package com.example.chesszimka.websocket.dto;

/** Отправляется обоим участникам сразу после принятия приглашения, до первого GAME_STATE. */
public class GameStartedMessage {
    public final String type = "GAME_STARTED";
    private String gameId;
    private String yourColor;

    /** Создаёт пустое сообщение (для десериализации). */
    public GameStartedMessage() {
    }

    /** Создаёт сообщение с id партии и назначенным цветом получателя. */
    public GameStartedMessage(String gameId, String yourColor) {
        this.gameId = gameId;
        this.yourColor = yourColor;
    }

    /** Возвращает тип сообщения ("GAME_STARTED"). */
    public String getType() {
        return type;
    }

    /** Возвращает id созданной партии. */
    public String getGameId() {
        return gameId;
    }

    /** Устанавливает id партии. */
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    /** Возвращает цвет, назначенный получателю сообщения. */
    public String getYourColor() {
        return yourColor;
    }

    /** Устанавливает цвет получателя. */
    public void setYourColor(String yourColor) {
        this.yourColor = yourColor;
    }
}
