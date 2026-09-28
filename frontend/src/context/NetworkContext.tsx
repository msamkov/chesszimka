import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { getOrCreateClientId } from '../lib/clientId';
import { getStoredName } from '../lib/names';

/** Игрок в списке lobby, доступный для приглашения. */
export interface LobbyPlayer {
  id: string;
  name: string;
}

/** Входящее приглашение сыграть партию от другого игрока. */
export interface Invitation {
  fromId: string;
  fromName: string;
}

/** Полное состояние сетевой партии, получаемое от сервера. */
export interface GameState {
  type: 'GAME_STATE';
  gameId: string;
  fen: string;
  turn: 'WHITE' | 'BLACK';
  status: 'IN_PROGRESS' | 'FINISHED';
  result: string;
  resultMessage: string | null;
  whiteName: string | null;
  blackName: string | null;
  yourColor: 'WHITE' | 'BLACK';
  inCheck: boolean;
  lastMoveFrom: string | null;
  lastMoveTo: string | null;
}

// Короткое уведомление (например, об отклонении приглашения), сопровождаемое ключом для повторного показа.
interface NoticeMsg {
  text: string;
  key: number;
}

// Значение, предоставляемое контекстом сетевого взаимодействия всем потребителям.
interface NetworkContextValue {
  connected: boolean;
  myId: string;
  lobbyPlayers: LobbyPlayer[];
  incomingInvitation: Invitation | null;
  outgoingInviteTargetId: string | null;
  notice: NoticeMsg | null;
  activeGame: GameState | null;
  error: string | null;
  invite: (targetId: string) => void;
  acceptInvite: () => void;
  declineInvite: () => void;
  cancelInvite: () => void;
  sendMove: (from: string, to: string, promotion?: string) => void;
  updateName: (name: string) => void;
  clearActiveGame: () => void;
}

const NetworkContext = createContext<NetworkContextValue | null>(null);

// Объединение всех возможных типов сообщений, приходящих от сервера по WebSocket.
type ServerMessage =
  | { type: 'LOBBY_LIST'; players: LobbyPlayer[] }
  | { type: 'INVITATION'; fromId: string; fromName: string }
  | { type: 'INVITE_DECLINED'; byId: string; byName: string }
  | { type: 'INVITE_CANCELLED'; byId: string; byName: string }
  | { type: 'GAME_STARTED'; gameId: string; yourColor: 'WHITE' | 'BLACK' }
  | (GameState)
  | { type: 'ERROR'; message: string };

/** Строит адрес WebSocket-сервера относительно текущего адреса страницы (важно для работы по LAN IP). */
function backendWsUrl(clientId: string, name: string): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws/app?clientId=${encodeURIComponent(clientId)}&name=${encodeURIComponent(name)}`;
}

/** Провайдер контекста сетевого взаимодействия: держит единственное WebSocket-соединение и состояние lobby/партии. */
export function NetworkProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const myId = useRef(getOrCreateClientId()).current;
  const socketRef = useRef<WebSocket | null>(null);
  const nameRef = useRef(getStoredName() || 'Игрок');
  const reconnectTimer = useRef<number | null>(null);

  const [connected, setConnected] = useState(false);
  const [lobbyPlayers, setLobbyPlayers] = useState<LobbyPlayer[]>([]);
  const [incomingInvitation, setIncomingInvitation] = useState<Invitation | null>(null);
  const [outgoingInviteTargetId, setOutgoingInviteTargetId] = useState<string | null>(null);
  const [notice, setNotice] = useState<NoticeMsg | null>(null);
  const [activeGame, setActiveGame] = useState<GameState | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Устанавливает WebSocket-соединение при монтировании и переподключается при разрыве.
  useEffect(() => {
    let cancelled = false;

    function connect() {
      if (cancelled) return;
      const socket = new WebSocket(backendWsUrl(myId, nameRef.current));
      socketRef.current = socket;

      socket.onopen = () => setConnected(true);
      socket.onclose = () => {
        setConnected(false);
        if (!cancelled) {
          reconnectTimer.current = window.setTimeout(connect, 2000);
        }
      };
      socket.onerror = () => {
        setError('Ошибка соединения с сервером.');
      };
      socket.onmessage = (event) => {
        let msg: ServerMessage;
        try {
          msg = JSON.parse(event.data);
        } catch {
          return;
        }
        switch (msg.type) {
          case 'LOBBY_LIST':
            setLobbyPlayers(msg.players);
            break;
          case 'INVITATION':
            setIncomingInvitation({ fromId: msg.fromId, fromName: msg.fromName });
            break;
          case 'INVITE_DECLINED':
            setOutgoingInviteTargetId(null);
            setNotice({ text: `${msg.byName} отклонил(а) приглашение.`, key: Date.now() });
            break;
          case 'INVITE_CANCELLED':
            setIncomingInvitation((current) => (current && current.fromId === msg.byId ? null : current));
            setOutgoingInviteTargetId(null);
            break;
          case 'GAME_STARTED':
            setIncomingInvitation(null);
            setOutgoingInviteTargetId(null);
            navigate(`/game/${msg.gameId}`);
            break;
          case 'GAME_STATE':
            setActiveGame(msg);
            setError(null);
            break;
          case 'ERROR':
            setError(msg.message);
            break;
        }
      };
    }

    connect();

    return () => {
      cancelled = true;
      if (reconnectTimer.current) {
        window.clearTimeout(reconnectTimer.current);
      }
      socketRef.current?.close();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Сериализует и отправляет сообщение на сервер, если соединение открыто.
  const send = useCallback((payload: unknown) => {
    const socket = socketRef.current;
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(payload));
    }
  }, []);

  /** Отправляет приглашение сыграть указанному игроку. */
  const invite = useCallback(
    (targetId: string) => {
      setOutgoingInviteTargetId(targetId);
      send({ type: 'INVITE', targetId });
    },
    [send],
  );

  /** Принимает текущее входящее приглашение. */
  const acceptInvite = useCallback(() => {
    if (!incomingInvitation) return;
    send({ type: 'ACCEPT_INVITE', fromId: incomingInvitation.fromId });
    setIncomingInvitation(null);
  }, [incomingInvitation, send]);

  /** Отклоняет текущее входящее приглашение. */
  const declineInvite = useCallback(() => {
    if (!incomingInvitation) return;
    send({ type: 'DECLINE_INVITE', fromId: incomingInvitation.fromId });
    setIncomingInvitation(null);
  }, [incomingInvitation, send]);

  /** Отменяет ранее отправленное собственное приглашение. */
  const cancelInvite = useCallback(() => {
    if (!outgoingInviteTargetId) return;
    send({ type: 'CANCEL_INVITE', targetId: outgoingInviteTargetId });
    setOutgoingInviteTargetId(null);
  }, [outgoingInviteTargetId, send]);

  /** Отправляет намерение сделать ход в текущей активной партии (сервер сам проверяет легальность). */
  const sendMove = useCallback(
    (from: string, to: string, promotion?: string) => {
      if (!activeGame) return;
      send({ type: 'MOVE', gameId: activeGame.gameId, from, to, promotion });
    },
    [activeGame, send],
  );

  /** Обновляет отображаемое имя игрока и сообщает об этом серверу. */
  const updateName = useCallback(
    (name: string) => {
      const finalName = name.trim().length > 0 ? name.trim() : nameRef.current;
      nameRef.current = finalName;
      send({ type: 'RENAME', name: finalName });
    },
    [send],
  );

  /** Сбрасывает текущую активную партию из состояния (например, при выходе на главный экран). */
  const clearActiveGame = useCallback(() => setActiveGame(null), []);

  const value: NetworkContextValue = {
    connected,
    myId,
    lobbyPlayers,
    incomingInvitation,
    outgoingInviteTargetId,
    notice,
    activeGame,
    error,
    invite,
    acceptInvite,
    declineInvite,
    cancelInvite,
    sendMove,
    updateName,
    clearActiveGame,
  };

  return <NetworkContext.Provider value={value}>{children}</NetworkContext.Provider>;
}

/** Хук доступа к сетевому контексту; должен использоваться только внутри NetworkProvider. */
export function useNetwork(): NetworkContextValue {
  const ctx = useContext(NetworkContext);
  if (!ctx) {
    throw new Error('useNetwork must be used within a NetworkProvider');
  }
  return ctx;
}
