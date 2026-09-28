package com.example.chesszimka.service;

import com.example.chesszimka.engine.ChessBoard;
import com.example.chesszimka.game.GameParticipant;
import com.example.chesszimka.game.GameResult;
import com.example.chesszimka.game.GameRoom;
import com.example.chesszimka.game.GameStatus;
import com.example.chesszimka.lobby.ConnectedUser;
import com.example.chesszimka.lobby.UserStatus;
import com.example.chesszimka.model.Color;
import com.example.chesszimka.model.Move;
import com.example.chesszimka.model.PieceType;
import com.example.chesszimka.websocket.dto.ErrorMessage;
import com.example.chesszimka.websocket.dto.StateMessage;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Владеет всеми текущими и завершёнными сетевыми партиями ({@link GameRoom}) и
 * применяет к ним ходы. Никогда не работает с «сырыми» WebSocket-сессиями напрямую —
 * вся доставка идёт через {@link UserRegistry#send(String, Object)} по clientId,
 * что прозрачно переживает переподключения (в реестре всегда самая свежая
 * сессия для клиента).
 */
@Service
public class GameService {

    private final Map<String, GameRoom> games = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final UserRegistry userRegistry;

    /** Создаёт сервис партий с внедрённым реестром пользователей. */
    public GameService(UserRegistry userRegistry) {
        this.userRegistry = userRegistry;
    }

    /** Возвращает партию по её id, либо null, если такой партии нет. */
    public GameRoom getGame(String gameId) {
        return games.get(gameId);
    }

    /**
     * Создаёт новую партию между двумя подключёнными пользователями, назначая
     * цвета случайным образом, и переводит обоих пользователей в статус PLAYING.
     * Должен вызываться под блокировкой реестра.
     */
    public GameRoom createGame(ConnectedUser a, ConnectedUser b) {
        String gameId = UUID.randomUUID().toString();
        GameParticipant pa = new GameParticipant(a.getId(), a.getName());
        GameParticipant pb = new GameParticipant(b.getId(), b.getName());
        GameRoom room = random.nextBoolean()
                ? new GameRoom(gameId, pa, pb)
                : new GameRoom(gameId, pb, pa);
        games.put(gameId, room);

        a.setStatus(UserStatus.PLAYING);
        a.setCurrentGameId(gameId);
        a.setPendingPeerId(null);
        b.setStatus(UserStatus.PLAYING);
        b.setCurrentGameId(gameId);
        b.setPendingPeerId(null);
        return room;
    }

    /** Проверяет право хода и легальность хода, применяет его к доске и рассылает обновлённое состояние. */
    public void applyMove(String gameId, String clientId, String from, String to, String promotionCode) {
        GameRoom room = games.get(gameId);
        if (room == null) {
            userRegistry.send(clientId, new ErrorMessage("Партия не найдена."));
            return;
        }
        room.getLock().lock();
        try {
            Color moverColor = room.colorOf(clientId);
            if (moverColor == null) {
                userRegistry.send(clientId, new ErrorMessage("Вы не участник этой партии."));
                return;
            }
            if (room.getStatus() != GameStatus.IN_PROGRESS) {
                userRegistry.send(clientId, new ErrorMessage("Партия уже завершена."));
                return;
            }
            ChessBoard board = room.getBoard();
            if (board.getSideToMove() != moverColor) {
                userRegistry.send(clientId, new ErrorMessage("Сейчас не ваш ход."));
                return;
            }
            if (from == null || to == null || from.length() != 2 || to.length() != 2) {
                userRegistry.send(clientId, new ErrorMessage("Некорректный ход."));
                return;
            }
            int fromRow = Move.rowFromSquare(from);
            int fromCol = Move.colFromSquare(from);
            int toRow = Move.rowFromSquare(to);
            int toCol = Move.colFromSquare(to);
            PieceType promotion = PieceType.fromPromotionCode(promotionCode);

            Move applied = board.tryMove(fromRow, fromCol, toRow, toCol, promotion);
            if (applied == null) {
                userRegistry.send(clientId, new ErrorMessage("Недопустимый ход."));
                return;
            }
            checkGameEnd(room);
            broadcastState(room);
        } finally {
            room.getLock().unlock();
        }
    }

    /** Проверяет, закончилась ли партия после хода, и если да — фиксирует результат и освобождает игроков. */
    private void checkGameEnd(GameRoom room) {
        ChessBoard board = room.getBoard();
        String reason = board.getLastResultReason();
        if (reason == null) {
            return;
        }
        room.setStatus(GameStatus.FINISHED);
        switch (reason) {
            case "CHECKMATE" -> {
                room.setResult(GameResult.CHECKMATE);
                room.setWinner(board.getSideToMove().opposite());
            }
            case "STALEMATE" -> room.setResult(GameResult.STALEMATE);
            case "DRAW_50" -> room.setResult(GameResult.DRAW_50_MOVES);
            case "DRAW_REPETITION" -> room.setResult(GameResult.DRAW_REPETITION);
            case "DRAW_MATERIAL" -> room.setResult(GameResult.DRAW_MATERIAL);
            default -> room.setResult(GameResult.NONE);
        }
        releasePlayersBackToLobby(room);
    }

    /** После нормального завершения партии (не из-за отключения) оба ещё подключённых игрока снова становятся READY. */
    private void releasePlayersBackToLobby(GameRoom room) {
        for (GameParticipant participant : room.getParticipants().values()) {
            ConnectedUser user = userRegistry.getUser(participant.clientId());
            if (user != null && user.getStatus() == UserStatus.PLAYING) {
                user.setStatus(UserStatus.READY);
                user.setCurrentGameId(null);
            }
        }
    }

    /**
     * Вызывается при отключении клиента, находившегося в статусе PLAYING.
     * Завершает партию (если она ещё не завершена) с результатом
     * OPPONENT_DISCONNECTED и уведомляет вторую сторону.
     */
    public void handlePlayerDisconnect(String clientId, String gameId) {
        if (gameId == null) {
            return;
        }
        GameRoom room = games.get(gameId);
        if (room == null) {
            return;
        }
        room.getLock().lock();
        try {
            Color disconnectedColor = room.colorOf(clientId);
            if (disconnectedColor == null || room.getStatus() == GameStatus.FINISHED) {
                return;
            }
            room.setStatus(GameStatus.FINISHED);
            room.setResult(GameResult.OPPONENT_DISCONNECTED);
            room.setWinner(disconnectedColor.opposite());
            broadcastState(room);

            GameParticipant opponent = room.getParticipant(disconnectedColor.opposite());
            ConnectedUser opponentUser = userRegistry.getUser(opponent.clientId());
            if (opponentUser != null && opponentUser.getStatus() == UserStatus.PLAYING) {
                opponentUser.setStatus(UserStatus.READY);
                opponentUser.setCurrentGameId(null);
            }
        } finally {
            room.getLock().unlock();
        }
    }

    /** Рассылает актуальное состояние партии обоим участникам. */
    public void broadcastState(GameRoom room) {
        for (Color color : Color.values()) {
            GameParticipant participant = room.getParticipant(color);
            if (participant != null) {
                sendState(room, participant, color);
            }
        }
    }

    /** Отправляет актуальное состояние партии одному конкретному клиенту (например, при переподключении). */
    public void sendStateTo(String clientId, GameRoom room) {
        Color color = room.colorOf(clientId);
        if (color == null) {
            return;
        }
        sendState(room, room.getParticipant(color), color);
    }

    /** Формирует и отправляет сообщение о состоянии партии конкретному получателю с учётом его цвета. */
    private void sendState(GameRoom room, GameParticipant recipient, Color recipientColor) {
        ChessBoard board = room.getBoard();
        StateMessage msg = new StateMessage();
        msg.setGameId(room.getId());
        msg.setFen(board.toFen());
        msg.setTurn(board.getSideToMove().name());
        msg.setStatus(room.getStatus().name());
        msg.setResult(room.getStatus() == GameStatus.FINISHED ? room.getResult().name() : "NONE");
        msg.setResultMessage(buildResultMessage(room));
        GameParticipant white = room.getParticipant(Color.WHITE);
        GameParticipant black = room.getParticipant(Color.BLACK);
        msg.setWhiteName(white != null ? white.name() : null);
        msg.setBlackName(black != null ? black.name() : null);
        msg.setYourColor(recipientColor.name());
        msg.setInCheck(room.getStatus() == GameStatus.IN_PROGRESS && board.isInCheck(board.getSideToMove()));
        Move lastMove = board.getLastMove();
        if (lastMove != null) {
            msg.setLastMoveFrom(lastMove.fromSquare());
            msg.setLastMoveTo(lastMove.toSquare());
        }
        userRegistry.send(recipient.clientId(), msg);
    }

    /** Формирует человекочитаемое сообщение об итоге завершённой партии. */
    private String buildResultMessage(GameRoom room) {
        if (room.getStatus() != GameStatus.FINISHED) {
            return null;
        }
        GameParticipant white = room.getParticipant(Color.WHITE);
        GameParticipant black = room.getParticipant(Color.BLACK);
        return switch (room.getResult()) {
            case CHECKMATE -> {
                GameParticipant winner = room.getWinner() == Color.WHITE ? white : black;
                yield "Мат. Победил(а) " + (winner != null ? winner.name() : "соперник") + ".";
            }
            case STALEMATE -> "Пат. Ничья.";
            case DRAW_50_MOVES -> "Ничья (правило 50 ходов).";
            case DRAW_REPETITION -> "Ничья (троекратное повторение позиции).";
            case DRAW_MATERIAL -> "Ничья (недостаточно материала для мата).";
            case OPPONENT_DISCONNECTED -> "Соперник отключился.";
            default -> "Партия завершена.";
        };
    }
}
