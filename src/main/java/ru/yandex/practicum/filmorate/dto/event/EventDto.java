package ru.yandex.practicum.filmorate.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;

@Data
@Builder
public class EventDto {
    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("eventType")
    private String eventType;  // возвращаем строку "LIKE", а не enum

    @JsonProperty("operation")
    private String operation;  // возвращаем строку "ADD"

    @JsonProperty("eventId")
    private Long eventId;

    @JsonProperty("entityId")
    private Long entityId;
}
