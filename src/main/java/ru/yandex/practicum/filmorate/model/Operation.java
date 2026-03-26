package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Operation {
    ADD("ADD"),
    REMOVE("REMOVE"),
    UPDATE("UPDATE");

    private final String value;
}



