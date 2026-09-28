package com.example.chesszimka.config;

import com.example.chesszimka.websocket.AppWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Регистрирует единственную постоянную WebSocket-точку входа приложения.
 * Разрешённые источники указаны как «*», поскольку у приложения нет
 * cookies/credentials и оно должно быть доступно как через localhost, так и
 * через IP-адрес в локальной сети (например, http://192.168.x.x:5173).
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AppWebSocketHandler appWebSocketHandler;

    /** Создаёт конфигурацию с внедрённым обработчиком WebSocket-сообщений. */
    public WebSocketConfig(AppWebSocketHandler appWebSocketHandler) {
        this.appWebSocketHandler = appWebSocketHandler;
    }

    /** Регистрирует обработчик по пути "/ws/app" с разрешением любых источников. */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(appWebSocketHandler, "/ws/app")
                .setAllowedOrigins("*");
    }
}
