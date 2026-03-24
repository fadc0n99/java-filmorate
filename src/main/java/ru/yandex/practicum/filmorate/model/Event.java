package ru.yandex.practicum.filmorate.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(of = { "eventId" })
public class Event {
    private Long eventId;      // primary key
    private Long timestamp;    // epoch milliseconds
    private Long userId;       // чья лента
    private EventType eventType;
    private Operation operation;
    private Long entityId;     // ID сущности (фильм, друг, отзыв)
}

