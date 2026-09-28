package com.example.chesszimka.websocket.dto;

/** Сообщение сервера об ошибке, отправляемое конкретному клиенту. */
public class ErrorMessage {
    public final String type = "ERROR";
    private String message;

    /** Создаёт пустое сообщение об ошибке (для десериализации). */
    public ErrorMessage() {
    }

    /** Создаёт сообщение об ошибке с текстом описания. */
    public ErrorMessage(String message) {
        this.message = message;
    }

    /** Возвращает тип сообщения ("ERROR"). */
    public String getType() {
        return type;
    }

    /** Возвращает текст ошибки. */
    public String getMessage() {
        return message;
    }

    /** Устанавливает текст ошибки. */
    public void setMessage(String message) {
        this.message = message;
    }
}

