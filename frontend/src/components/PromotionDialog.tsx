// Параметры диалога выбора фигуры для превращения пешки.
interface PromotionDialogProps {
  color: 'w' | 'b';
  onSelect: (piece: 'q' | 'r' | 'b' | 'n') => void;
}

// Список фигур, доступных для превращения, с символами для белых и чёрных.
const PIECES: { code: 'q' | 'r' | 'b' | 'n'; whiteChar: string; blackChar: string; label: string }[] = [
  { code: 'q', whiteChar: '♕', blackChar: '♛', label: 'Ферзь' },
  { code: 'r', whiteChar: '♖', blackChar: '♜', label: 'Ладья' },
  { code: 'b', whiteChar: '♗', blackChar: '♝', label: 'Слон' },
  { code: 'n', whiteChar: '♘', blackChar: '♞', label: 'Конь' },
];

/** Модальное окно выбора фигуры при превращении пешки. */
export function PromotionDialog({ color, onSelect }: PromotionDialogProps) {
  return (
    <div className="promotion-overlay">
      <div className="promotion-box">
        <h2>Выберите фигуру</h2>
        <div className="promotion-pieces">
          {PIECES.map((p) => (
            <button key={p.code} title={p.label} onClick={() => onSelect(p.code)}>
              {color === 'w' ? p.whiteChar : p.blackChar}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
