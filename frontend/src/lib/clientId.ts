// Ключ localStorage, под которым хранится стабильный id клиента.
const CLIENT_ID_KEY = 'chess_client_id';

/** Стабильный для браузера id, переживающий перезагрузку страницы, используется для идентификации клиента сервером. */
export function getOrCreateClientId(): string {
  let id = localStorage.getItem(CLIENT_ID_KEY);
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem(CLIENT_ID_KEY, id);
  }
  return id;
}
