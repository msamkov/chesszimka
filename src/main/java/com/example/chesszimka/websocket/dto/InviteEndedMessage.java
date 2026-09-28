package com.example.chesszimka.websocket.dto;

/**
 * Отправляется, когда незавершённое приглашение разрешилось без начала партии:
 * type — либо "INVITE_DECLINED" (приглашённый отказался), либо
 * "INVITE_CANCELLED" (пригласивший отозвал приглашение).
 */
public class InviteEndedMessage {
    private final String type;
    private String byId;
    private String byName;

    /** Создаёт сообщение о завершении приглашения с указанием инициатора действия. */
    public InviteEndedMessage(String type, String byId, String byName) {
        this.type = type;
        this.byId = byId;
        this.byName = byName;
    }

    /** Возвращает тип события ("INVITE_DECLINED" или "INVITE_CANCELLED"). */
    public String getType() {
        return type;
    }

    /** Возвращает id игрока, инициировавшего отклонение/отмену. */
    public String getById() {
        return byId;
    }

    /** Устанавливает id инициатора. */
    public void setById(String byId) {
        this.byId = byId;
    }

    /** Возвращает имя игрока, инициировавшего отклонение/отмену. */
    public String getByName() {
        return byName;
    }

    /** Устанавливает имя инициатора. */
    public void setByName(String byName) {
        this.byName = byName;
    }
}
