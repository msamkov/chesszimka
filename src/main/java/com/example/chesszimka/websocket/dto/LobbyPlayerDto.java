package com.example.chesszimka.websocket.dto;

/** Краткое представление игрока в lobby (id и имя) для передачи клиенту. */
public class LobbyPlayerDto {
    private String id;
    private String name;

    /** Создаёт пустой DTO (для десериализации). */
    public LobbyPlayerDto() {
    }

    /** Создаёт DTO с id и именем игрока. */
    public LobbyPlayerDto(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /** Возвращает id игрока. */
    public String getId() {
        return id;
    }

    /** Устанавливает id игрока. */
    public void setId(String id) {
        this.id = id;
    }

    /** Возвращает имя игрока. */
    public String getName() {
        return name;
    }

    /** Устанавливает имя игрока. */
    public void setName(String name) {
        this.name = name;
    }
}
