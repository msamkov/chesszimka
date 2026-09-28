package com.example.chesszimka.service;

import com.example.chesszimka.lobby.ConnectedUser;
import com.example.chesszimka.lobby.UserStatus;
import com.example.chesszimka.websocket.dto.LobbyListMessage;
import com.example.chesszimka.websocket.dto.LobbyPlayerDto;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Центральный in-memory реестр всех вкладок браузера, подключённых к приложению
 * сейчас, с ключом по сгенерированному на клиенте id, который переживает
 * перезагрузку страницы. Также отвечает за низкоуровневую доставку JSON по
 * WebSocket-сессии каждого пользователя. Любые последовательности
 * чтение-изменение-запись, затрагивающие более одного пользователя (например,
 * приглашения), должны выполняться под {@link #getLock()}.
 */
@Component
public class UserRegistry {

    private final Map<String, ConnectedUser> usersById = new ConcurrentHashMap<>();
    private final Map<String, String> clientIdBySessionId = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Возвращает общую блокировку для операций, затрагивающих нескольких пользователей. */
    public ReentrantLock getLock() {
        return lock;
    }

    /**
     * Регистрирует новый client id или переподключает сессию к уже известному
     * client id (реконнект). Возвращает актуальный {@link ConnectedUser}.
     */
    public ConnectedUser connect(String clientId, String name, WebSocketSession session) {
        ConnectedUser user = usersById.computeIfAbsent(clientId, id -> new ConnectedUser(id, name, session));
        user.setSession(session);
        user.setName(name);
        clientIdBySessionId.put(session.getId(), clientId);
        return user;
    }

    /** Возвращает пользователя по client id, либо null, если он не зарегистрирован. */
    public ConnectedUser getUser(String clientId) {
        return usersById.get(clientId);
    }

    /** Возвращает client id, связанный с данной WebSocket-сессией. */
    public String getClientIdBySession(WebSocketSession session) {
        return clientIdBySessionId.get(session.getId());
    }

    /** Удаляет привязку сессии к client id (при закрытии соединения). */
    public void forgetSession(WebSocketSession session) {
        clientIdBySessionId.remove(session.getId());
    }

    /** Возвращает снимок всех зарегистрированных пользователей. */
    public List<ConnectedUser> allUsers() {
        return List.copyOf(usersById.values());
    }

    /** Отправляет персональный (без себя самого) список lobby каждому пользователю в статусе READY. */
    public void broadcastLobbyToAll() {
        List<ConnectedUser> readyUsers = usersById.values().stream()
                .filter(u -> u.getStatus() == UserStatus.READY)
                .toList();
        for (ConnectedUser recipient : readyUsers) {
            List<LobbyPlayerDto> others = readyUsers.stream()
                    .filter(u -> !u.getId().equals(recipient.getId()))
                    .map(u -> new LobbyPlayerDto(u.getId(), u.getName()))
                    .collect(Collectors.toList());
            send(recipient.getId(), new LobbyListMessage(others));
        }
    }

    /** Отправляет сообщение пользователю по его client id (если он подключён). */
    public void send(String clientId, Object payload) {
        ConnectedUser user = usersById.get(clientId);
        if (user == null) {
            return;
        }
        sendToSession(user.getSession(), payload);
    }

    /** Сериализует и отправляет сообщение в конкретную WebSocket-сессию. */
    public void sendToSession(WebSocketSession session, Object payload) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception ignored) {
            // Сессия, вероятно, закрылась параллельно; ничего не делаем.
        }
    }
}
