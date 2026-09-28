package com.example.chesszimka.websocket.dto;

import java.util.List;

/** Отправляется каждому пользователю в статусе READY при изменении пула доступных соперников. */
public class LobbyListMessage {
    public final String type = "LOBBY_LIST";
    private List<LobbyPlayerDto> players;

    /** Создаёт пустое сообщение (для десериализации). */
    public LobbyListMessage() {
    }

    /** Создаёт сообщение со списком доступных игроков. */
    public LobbyListMessage(List<LobbyPlayerDto> players) {
        this.players = players;
    }

    /** Возвращает тип сообщения ("LOBBY_LIST"). */
    public String getType() {
        return type;
    }

    /** Возвращает список доступных для игры соперников. */
    public List<LobbyPlayerDto> getPlayers() {
        return players;
    }

    /** Устанавливает список доступных соперников. */
    public void setPlayers(List<LobbyPlayerDto> players) {
        this.players = players;
    }
}
