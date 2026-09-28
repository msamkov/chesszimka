package com.example.chesszimka.websocket.dto;

/** Отправляется приглашённому игроку, когда кто-то хочет сыграть с ним партию. */
public class InvitationMessage {
    public final String type = "INVITATION";
    private String fromId;
    private String fromName;

    /** Создаёт пустое сообщение (для десериализации). */
    public InvitationMessage() {
    }

    /** Создаёт сообщение с id и именем пригласившего игрока. */
    public InvitationMessage(String fromId, String fromName) {
        this.fromId = fromId;
        this.fromName = fromName;
    }

    /** Возвращает тип сообщения ("INVITATION"). */
    public String getType() {
        return type;
    }

    /** Возвращает id пригласившего игрока. */
    public String getFromId() {
        return fromId;
    }

    /** Устанавливает id пригласившего игрока. */
    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    /** Возвращает имя пригласившего игрока. */
    public String getFromName() {
        return fromName;
    }

    /** Устанавливает имя пригласившего игрока. */
    public void setFromName(String fromName) {
        this.fromName = fromName;
    }
}
