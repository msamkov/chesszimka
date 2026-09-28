package com.example.chesszimka.lobby;

import org.springframework.web.socket.WebSocketSession;

/**
 * Представляет одного реального пользователя приложения, идентифицируемого
 * сгенерированным на клиенте id, который сохраняется при переподключениях
 * (хранится в localStorage браузера). Все изменяемые поля модифицируются
 * только под блокировкой {@link UserRegistry}.
 */
public class ConnectedUser {

    private final String id;
    private String name;
    private WebSocketSession session;
    private UserStatus status;
    private String currentGameId;
    /** Пока статус == INVITED — id второй стороны в незавершённом приглашении. */
    private String pendingPeerId;

    /** Создаёт нового пользователя со статусом ONLINE. */
    public ConnectedUser(String id, String name, WebSocketSession session) {
        this.id = id;
        this.name = name;
        this.session = session;
        this.status = UserStatus.ONLINE;
    }

    /** Возвращает стабильный идентификатор клиента. */
    public String getId() {
        return id;
    }

    /** Возвращает отображаемое имя игрока. */
    public String getName() {
        return name;
    }

    /** Устанавливает отображаемое имя игрока. */
    public void setName(String name) {
        this.name = name;
    }

    /** Возвращает текущую (актуальную после реконнекта) WebSocket-сессию. */
    public WebSocketSession getSession() {
        return session;
    }

    /** Заменяет WebSocket-сессию (используется при переподключении). */
    public void setSession(WebSocketSession session) {
        this.session = session;
    }

    /** Возвращает текущий статус пользователя. */
    public UserStatus getStatus() {
        return status;
    }

    /** Устанавливает статус пользователя. */
    public void setStatus(UserStatus status) {
        this.status = status;
    }

    /** Возвращает id партии, в которой участвует пользователь, либо null. */
    public String getCurrentGameId() {
        return currentGameId;
    }

    /** Устанавливает id текущей партии пользователя. */
    public void setCurrentGameId(String currentGameId) {
        this.currentGameId = currentGameId;
    }

    /** Возвращает id второй стороны незавершённого приглашения. */
    public String getPendingPeerId() {
        return pendingPeerId;
    }

    /** Устанавливает id второй стороны незавершённого приглашения. */
    public void setPendingPeerId(String pendingPeerId) {
        this.pendingPeerId = pendingPeerId;
    }
}
