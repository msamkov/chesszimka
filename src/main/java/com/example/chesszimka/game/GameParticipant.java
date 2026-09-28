package com.example.chesszimka.game;

/** Лёгкий неизменяемый снимок одной из сторон сетевой партии (только id и имя, без живой сессии). */
public record GameParticipant(String clientId, String name) {
}
