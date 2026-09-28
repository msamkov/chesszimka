// Список смешных имён, которые назначаются автоматически, если игрок не ввёл своё.
const FUNNY_NAMES = [
  'Конь в пальто',
  'Ладья на районе',
  'Пешка-терминатор',
  'Гроссмейстер Вася',
  'Шахматный пельмень',
  'Слон без тормозов',
  'Ферзь-объедала',
  'Король на диете',
  'Битый небитый',
  'Мат в четыре хода',
];

/** Возвращает случайное смешное имя из заранее заданного списка. */
export function randomFunnyName(): string {
  return FUNNY_NAMES[Math.floor(Math.random() * FUNNY_NAMES.length)];
}

// Ключ localStorage, под которым хранится выбранное имя игрока.
const NAME_STORAGE_KEY = 'chess_player_name';

/** Возвращает ранее сохранённое имя игрока (или пустую строку, если ещё не сохранялось). */
export function getStoredName(): string {
  return localStorage.getItem(NAME_STORAGE_KEY) ?? '';
}

/** Сохраняет имя игрока в localStorage, подставляя случайное смешное имя, если строка пустая. */
export function storeName(name: string): string {
  const finalName = name.trim().length > 0 ? name.trim() : randomFunnyName();
  localStorage.setItem(NAME_STORAGE_KEY, finalName);
  return finalName;
}
