package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventType {
    LIKE("LIKE"),
    REVIEW("REVIEW"),
    FRIEND("FRIEND");

    private final String value;
}


