import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getStoredName, randomFunnyName, storeName } from '../lib/names';
import { useNetwork } from '../context/NetworkContext';

/** Главный экран приложения: ввод имени и выбор режима игры. */
export function HomePage() {
  const [name, setName] = useState(getStoredName());
  const navigate = useNavigate();
  const { updateName } = useNetwork();

  // Сохраняет имя (или подставляет случайное смешное), сообщает о нём серверу и переходит на нужный экран.
  function goTo(path: string) {
    const finalName = storeName(name);
    updateName(finalName);
    navigate(path);
  }

  return (
    <div className="card">
      <h2>Твой позывной</h2>
      <input
        type="text"
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder={`Например: ${randomFunnyName()}`}
        maxLength={40}
      />
      <p className="hint">Если оставить поле пустым — получишь случайное смешное имя.</p>
      <div className="button-row" style={{ flexDirection: 'column' }}>
        <button className="primary" onClick={() => goTo('/computer')}>
          Играть с компьютером
        </button>
        <button className="primary" onClick={() => goTo('/network')}>
          Играть по сети
        </button>
      </div>
    </div>
  );
}
