package com.example.chesszimka.websocket.dto;

/**
 * Любое сообщение, отправляемое клиентом серверу по единственному постоянному
 * WebSocket-соединению приложения. Заполняются только поля, актуальные для
 * конкретного {@code type}:
 * <ul>
 *   <li>INVITE - targetId</li>
 *   <li>ACCEPT_INVITE / DECLINE_INVITE - fromId (id пригласившего)</li>
 *   <li>CANCEL_INVITE - targetId (id приглашённого)</li>
 *   <li>RENAME - name</li>
 *   <li>MOVE - gameId, from, to, promotion (опционально)</li>
 * </ul>
 */
public class ClientMessage {
    private String type;
    private String targetId;
    private String fromId;
    private String gameId;
    private String from;
    private String to;
    private String promotion;
    private String name;

    /** Возвращает тип сообщения. */
    public String getType() {
        return type;
    }

    /** Устанавливает тип сообщения. */
    public void setType(String type) {
        this.type = type;
    }

    /** Возвращает id целевого игрока (для INVITE/CANCEL_INVITE). */
    public String getTargetId() {
        return targetId;
    }

    /** Устанавливает id целевого игрока. */
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    /** Возвращает id пригласившего игрока (для ACCEPT_INVITE/DECLINE_INVITE). */
    public String getFromId() {
        return fromId;
    }

    /** Устанавливает id пригласившего игрока. */
    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    /** Возвращает id партии (для сообщений о ходах). */
    public String getGameId() {
        return gameId;
    }

    /** Устанавливает id партии. */
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    /** Возвращает клетку начала хода. */
    public String getFrom() {
        return from;
    }

    /** Устанавливает клетку начала хода. */
    public void setFrom(String from) {
        this.from = from;
    }

    /** Возвращает клетку окончания хода. */
    public String getTo() {
        return to;
    }

    /** Устанавливает клетку окончания хода. */
    public void setTo(String to) {
        this.to = to;
    }

    /** Возвращает код превращения пешки (q/r/b/n), если применимо. */
    public String getPromotion() {
        return promotion;
    }

    /** Устанавливает код превращения пешки. */
    public void setPromotion(String promotion) {
        this.promotion = promotion;
    }

    /** Возвращает новое имя игрока (для RENAME). */
    public String getName() {
        return name;
    }

    /** Устанавливает новое имя игрока. */
    public void setName(String name) {
        this.name = name;
    }
}
