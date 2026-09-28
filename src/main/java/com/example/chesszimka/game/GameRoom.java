package com.example.chesszimka.game;

import com.example.chesszimka.engine.ChessBoard;
import com.example.chesszimka.model.Color;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Представление сетевой партии в памяти. Оба участника известны уже в момент
 * создания комнаты (приглашения принимаются до того, как GameRoom вообще
 * создаётся), поэтому состояния «ожидание соперника» не существует. Все
 * изменения выполняются под блокировкой комнаты, чтобы безопасно обрабатывать
 * одновременные WebSocket-сообщения.
 */
public class GameRoom {

    private final String id;
    private final ChessBoard board = new ChessBoard();
    private final Map<Color, GameParticipant> participants = new EnumMap<>(Color.class);
    private GameStatus status = GameStatus.IN_PROGRESS;
    private GameResult result = GameResult.NONE;
    private Color winner; // null означает ничью
    private final ReentrantLock lock = new ReentrantLock();

    /** Создаёт новую партию с уже известными белым и чёрным участниками. */
    public GameRoom(String id, GameParticipant white, GameParticipant black) {
        this.id = id;
        participants.put(Color.WHITE, white);
        participants.put(Color.BLACK, black);
    }

    /** Возвращает идентификатор партии. */
    public String getId() {
        return id;
    }

    /** Возвращает шахматную доску партии. */
    public ChessBoard getBoard() {
        return board;
    }

    /** Возвращает карту участников по цвету. */
    public Map<Color, GameParticipant> getParticipants() {
        return participants;
    }

    /** Возвращает участника, играющего указанным цветом. */
    public GameParticipant getParticipant(Color color) {
        return participants.get(color);
    }

    /** Возвращает цвет, которым играет указанный клиент, либо null, если он не участник этой партии. */
    public Color colorOf(String clientId) {
        for (Map.Entry<Color, GameParticipant> entry : participants.entrySet()) {
            if (entry.getValue().clientId().equals(clientId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Возвращает текущий статус партии. */
    public GameStatus getStatus() {
        return status;
    }

    /** Устанавливает статус партии. */
    public void setStatus(GameStatus status) {
        this.status = status;
    }

    /** Возвращает результат партии. */
    public GameResult getResult() {
        return result;
    }

    /** Устанавливает результат партии. */
    public void setResult(GameResult result) {
        this.result = result;
    }

    /** Возвращает победителя (null, если ничья или партия не завершена). */
    public Color getWinner() {
        return winner;
    }

    /** Устанавливает победителя партии. */
    public void setWinner(Color winner) {
        this.winner = winner;
    }

    /** Возвращает блокировку для потокобезопасного изменения состояния партии. */
    public ReentrantLock getLock() {
        return lock;
    }
}
