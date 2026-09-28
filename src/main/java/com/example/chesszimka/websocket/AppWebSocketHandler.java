package com.example.chesszimka.websocket;

import com.example.chesszimka.service.LobbyService;
import com.example.chesszimka.websocket.dto.ClientMessage;
import com.example.chesszimka.websocket.dto.ErrorMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Единственная постоянная WebSocket-точка входа на всё время жизни приложения
 * (устанавливается сразу при загрузке страницы, по адресу
 * /ws/app?clientId=...&name=...). Обрабатывает регистрацию/переподключение в
 * lobby, приглашения и игровые ходы — всё, что нужно клиенту, происходит через
 * одно это соединение.
 */
@Component
public class AppWebSocketHandler extends TextWebSocketHandler {

    private static final String CLIENT_ID_ATTR = "clientId";

    private final LobbyService lobbyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Создаёт обработчик с внедрённым сервисом lobby. */
    public AppWebSocketHandler(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    /** Вызывается при установлении соединения: читает clientId/имя из query-параметров и регистрирует пользователя. */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Map<String, String> query = parseQuery(uriOf(session));
        String clientId = query.get("clientId");
        String name = query.get("name");
        if (clientId == null || clientId.isBlank()) {
            closeQuietly(session, CloseStatus.BAD_DATA);
            return;
        }
        session.getAttributes().put(CLIENT_ID_ATTR, clientId);
        lobbyService.onConnect(clientId, name != null && !name.isBlank() ? name : "Игрок", session);
    }

    /** Десериализует входящее текстовое сообщение и передаёт его на обработку в сервис lobby. */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
        String clientId = (String) session.getAttributes().get(CLIENT_ID_ATTR);
        if (clientId == null) {
            return;
        }
        try {
            ClientMessage message = objectMapper.readValue(textMessage.getPayload(), ClientMessage.class);
            lobbyService.onMessage(clientId, message);
        } catch (Exception e) {
            sendQuietly(session, new ErrorMessage("Некорректное сообщение."));
        }
    }

    /** Вызывается при закрытии соединения (любой причиной) и делегирует обработку сервису lobby. */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        lobbyService.onDisconnect(session);
    }

    /** Возвращает URI сессии либо URI-заглушку, если он недоступен. */
    private URI uriOf(WebSocketSession session) {
        return session.getUri() != null ? session.getUri() : URI.create("/ws/app");
    }

    /** Разбирает строку query-параметров URI в карту ключ-значение с декодированием URL-кодирования. */
    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> result = new LinkedHashMap<>();
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return result;
        }
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            String key = idx >= 0 ? pair.substring(0, idx) : pair;
            String value = idx >= 0 ? pair.substring(idx + 1) : "";
            result.put(URLDecoder.decode(key, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return result;
    }

    /** Отправляет сообщение в сессию, молча игнорируя ошибки (сессия могла уже закрыться). */
    private void sendQuietly(WebSocketSession session, Object payload) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception ignored) {
        }
    }

    /** Закрывает сессию с указанным статусом, молча игнорируя ошибки. */
    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception ignored) {
        }
    }
}
