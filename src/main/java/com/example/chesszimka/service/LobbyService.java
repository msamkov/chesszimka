package com.example.chesszimka.service;

import com.example.chesszimka.game.GameRoom;
import com.example.chesszimka.game.GameStatus;
import com.example.chesszimka.lobby.ConnectedUser;
import com.example.chesszimka.lobby.UserStatus;
import com.example.chesszimka.websocket.dto.ClientMessage;
import com.example.chesszimka.websocket.dto.ErrorMessage;
import com.example.chesszimka.websocket.dto.GameStartedMessage;
import com.example.chesszimka.websocket.dto.InvitationMessage;
import com.example.chesszimka.websocket.dto.InviteEndedMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Управляет жизненным циклом lobby (подключение/переподключение/отключение), а
 * также всеми типами сообщений, не связанными с ходами, поступающими по
 * постоянному WebSocket-соединению приложения: приглашениями, их принятием,
 * отклонением, отменой, а также сменой имени игрока. Шахматные ходы
 * делегируются напрямую в {@link GameService}.
 */
@Service
public class LobbyService {

    /** Сколько времени терпим отключение посреди партии, прежде чем сообщить сопернику об окончании игры. */
    private static final int RECONNECT_GRACE_SECONDS = 10;

    private final UserRegistry userRegistry;
    private final GameService gameService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "reconnect-grace-timer");
        t.setDaemon(true);
        return t;
    });

    /** Создаёт сервис lobby с внедрёнными реестром пользователей и сервисом партий. */
    public LobbyService(UserRegistry userRegistry, GameService gameService) {
        this.userRegistry = userRegistry;
        this.gameService = gameService;
    }

    /** Останавливает фоновый планировщик таймеров реконнекта при завершении работы бина. */
    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }

    /** Вызывается сразу после установления WebSocket-соединения клиента (в том числе при реконнекте). */
    public void onConnect(String clientId, String name, WebSocketSession session) {
        userRegistry.getLock().lock();
        try {
            ConnectedUser user = userRegistry.connect(clientId, name, session);
            String gameId = user.getCurrentGameId();
            if (gameId != null) {
                GameRoom room = gameService.getGame(gameId);
                if (room != null && room.getStatus() == GameStatus.IN_PROGRESS) {
                    // Переподключение посреди партии (в пределах грейс-периода): восстанавливаем
                    // PLAYING и отправляем актуальное состояние, не трогая список lobby.
                    user.setStatus(UserStatus.PLAYING);
                    gameService.sendStateTo(clientId, room);
                    return;
                }
                // Устаревшая ссылка (партия завершилась/исчезла, пока клиент отсутствовал) - возвращаем в lobby.
                user.setCurrentGameId(null);
            }
            user.setPendingPeerId(null);
            user.setStatus(UserStatus.READY);
        } finally {
            userRegistry.getLock().unlock();
        }
        userRegistry.broadcastLobbyToAll();
    }

    /** Диспетчеризует входящее сообщение клиента по типу. */
    public void onMessage(String clientId, ClientMessage message) {
        if (clientId == null || message == null || message.getType() == null) {
            return;
        }
        switch (message.getType()) {
            case "INVITE" -> handleInvite(clientId, message.getTargetId());
            case "ACCEPT_INVITE" -> handleAccept(clientId, message.getFromId());
            case "DECLINE_INVITE" -> handleDecline(clientId, message.getFromId());
            case "CANCEL_INVITE" -> handleCancel(clientId, message.getTargetId());
            case "RENAME" -> handleRename(clientId, message.getName());
            case "MOVE" -> gameService.applyMove(message.getGameId(), clientId, message.getFrom(), message.getTo(), message.getPromotion());
            default -> userRegistry.send(clientId, new ErrorMessage("Неизвестный тип сообщения."));
        }
    }

    /** Обрабатывает отправку приглашения одним игроком другому. */
    private void handleInvite(String clientId, String targetId) {
        if (targetId == null) {
            return;
        }
        userRegistry.getLock().lock();
        try {
            ConnectedUser inviter = userRegistry.getUser(clientId);
            ConnectedUser target = userRegistry.getUser(targetId);
            if (inviter == null || target == null || inviter.getId().equals(target.getId())) {
                return;
            }
            if (inviter.getStatus() != UserStatus.READY || target.getStatus() != UserStatus.READY) {
                userRegistry.send(clientId, new ErrorMessage("Этот игрок сейчас недоступен."));
                return;
            }
            inviter.setStatus(UserStatus.INVITED);
            inviter.setPendingPeerId(target.getId());
            target.setStatus(UserStatus.INVITED);
            target.setPendingPeerId(inviter.getId());
            userRegistry.send(target.getId(), new InvitationMessage(inviter.getId(), inviter.getName()));
        } finally {
            userRegistry.getLock().unlock();
        }
        userRegistry.broadcastLobbyToAll();
    }

    /** Обрабатывает принятие приглашения: создаёт партию и переводит обоих игроков в игру. */
    private void handleAccept(String clientId, String fromId) {
        if (fromId == null) {
            return;
        }
        userRegistry.getLock().lock();
        try {
            ConnectedUser accepter = userRegistry.getUser(clientId);
            ConnectedUser inviter = userRegistry.getUser(fromId);
            if (accepter == null || inviter == null) {
                return;
            }
            if (!isValidPendingPair(accepter, inviter)) {
                userRegistry.send(clientId, new ErrorMessage("Приглашение больше не действительно."));
                return;
            }
            GameRoom room = gameService.createGame(inviter, accepter);
            userRegistry.send(inviter.getId(),
                    new GameStartedMessage(room.getId(), room.colorOf(inviter.getId()).name()));
            userRegistry.send(accepter.getId(),
                    new GameStartedMessage(room.getId(), room.colorOf(accepter.getId()).name()));
            gameService.broadcastState(room);
        } finally {
            userRegistry.getLock().unlock();
        }
        userRegistry.broadcastLobbyToAll();
    }

    /** Обрабатывает отклонение приглашения: оба участника возвращаются в READY. */
    private void handleDecline(String clientId, String fromId) {
        if (fromId == null) {
            return;
        }
        userRegistry.getLock().lock();
        try {
            ConnectedUser decliner = userRegistry.getUser(clientId);
            ConnectedUser inviter = userRegistry.getUser(fromId);
            if (decliner == null || inviter == null || !isValidPendingPair(decliner, inviter)) {
                return;
            }
            resetToReady(decliner);
            resetToReady(inviter);
            userRegistry.send(inviter.getId(), new InviteEndedMessage("INVITE_DECLINED", decliner.getId(), decliner.getName()));
        } finally {
            userRegistry.getLock().unlock();
        }
        userRegistry.broadcastLobbyToAll();
    }

    /** Обрабатывает отмену приглашения инициатором. */
    private void handleCancel(String clientId, String targetId) {
        if (targetId == null) {
            return;
        }
        userRegistry.getLock().lock();
        try {
            ConnectedUser canceller = userRegistry.getUser(clientId);
            ConnectedUser target = userRegistry.getUser(targetId);
            if (canceller == null || target == null || !isValidPendingPair(canceller, target)) {
                return;
            }
            resetToReady(canceller);
            resetToReady(target);
            userRegistry.send(target.getId(), new InviteEndedMessage("INVITE_CANCELLED", canceller.getId(), canceller.getName()));
        } finally {
            userRegistry.getLock().unlock();
        }
        userRegistry.broadcastLobbyToAll();
    }

    /** Проверяет, что двое пользователей действительно связаны незавершённым приглашением друг с другом. */
    private boolean isValidPendingPair(ConnectedUser one, ConnectedUser two) {
        return one.getStatus() == UserStatus.INVITED
                && two.getStatus() == UserStatus.INVITED
                && two.getId().equals(one.getPendingPeerId())
                && one.getId().equals(two.getPendingPeerId());
    }

    /** Сбрасывает пользователя обратно в статус READY и очищает ссылку на ожидающего собеседника. */
    private void resetToReady(ConnectedUser user) {
        user.setStatus(UserStatus.READY);
        user.setPendingPeerId(null);
    }

    /** Обрабатывает смену отображаемого имени пользователя. */
    private void handleRename(String clientId, String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        userRegistry.getLock().lock();
        try {
            ConnectedUser user = userRegistry.getUser(clientId);
            if (user == null) {
                return;
            }
            user.setName(name.trim());
        } finally {
            userRegistry.getLock().unlock();
        }
        userRegistry.broadcastLobbyToAll();
    }

    /** Вызывается при закрытии WebSocket-соединения независимо от причины. */
    public void onDisconnect(WebSocketSession session) {
        String clientId = userRegistry.getClientIdBySession(session);
        userRegistry.forgetSession(session);
        if (clientId == null) {
            return;
        }
        boolean scheduleGameGracePeriod = false;
        userRegistry.getLock().lock();
        try {
            ConnectedUser user = userRegistry.getUser(clientId);
            if (user == null) {
                return;
            }
            // Игнорируем устаревшее событие закрытия от сессии, уже заменённой реконнектом.
            if (user.getSession() != null && !user.getSession().getId().equals(session.getId())) {
                return;
            }
            UserStatus previousStatus = user.getStatus();
            String peerId = user.getPendingPeerId();
            user.setStatus(UserStatus.OFFLINE);

            if (previousStatus == UserStatus.PLAYING && user.getCurrentGameId() != null) {
                // Не завершаем партию немедленно - даём браузеру небольшое окно для
                // переподключения (например, кратковременный сбой сети или перезагрузка страницы),
                // прежде чем считать соперника окончательно отключившимся.
                scheduleGameGracePeriod = true;
            } else if (previousStatus == UserStatus.INVITED && peerId != null) {
                ConnectedUser peer = userRegistry.getUser(peerId);
                if (peer != null) {
                    resetToReady(peer);
                    userRegistry.send(peer.getId(), new InviteEndedMessage("INVITE_CANCELLED", user.getId(), user.getName()));
                }
            }
        } finally {
            userRegistry.getLock().unlock();
        }
        userRegistry.broadcastLobbyToAll();
        if (scheduleGameGracePeriod) {
            scheduler.schedule(() -> finalizeGameDisconnectIfStillOffline(clientId),
                    RECONNECT_GRACE_SECONDS, TimeUnit.SECONDS);
        }
    }

    /** Выполняется после истечения грейс-периода; завершает партию, только если клиент так и не переподключился. */
    private void finalizeGameDisconnectIfStillOffline(String clientId) {
        String gameId;
        userRegistry.getLock().lock();
        try {
            ConnectedUser user = userRegistry.getUser(clientId);
            if (user == null || user.getStatus() != UserStatus.OFFLINE) {
                return; // Переподключился (или статус изменился) за время грейс-периода - ничего делать не нужно.
            }
            gameId = user.getCurrentGameId();
        } finally {
            userRegistry.getLock().unlock();
        }
        if (gameId != null) {
            gameService.handlePlayerDisconnect(clientId, gameId);
        }
        userRegistry.broadcastLobbyToAll();
    }
}
