package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.event.EventDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final EventStorage eventStorage;
    private final UserService userService;

    public List<EventDto> getUserFeed(Long userId, Integer count) {
        log.debug("Getting feed for user {}, limit {}", userId, count);

        // Простая валидация без вызова userService.isUserExists()
        if (userId == null || userId <= 0) {
            throw new NotFoundException("Invalid user ID: must be greater than 0");
        }

        List<Event> events = eventStorage.findFeedByUserId(userId, count != null ? count : 10);

        return events.stream()
                .map(this::convertToDto)
                .toList();
    }

    private EventDto convertToDto(Event event) {
        return EventDto.builder()
                .eventId(event.getEventId())
                .timestamp(event.getTimestamp())
                .userId(event.getUserId())
                .eventType(event.getEventType().getValue())
                .operation(event.getOperation().getValue())
                .entityId(event.getEntityId())
                .build();
    }

    // Публичный метод для логирования событий из других сервисов
    public void logEvent(Long userId, EventType type, Operation op, Long entityId) {
        // Простая валидация
        if (userId == null || userId <= 0) {
            throw new NotFoundException("Invalid user ID: must be greater than 0");
        }

        Event event = Event.builder()
                .timestamp(System.currentTimeMillis())
                .userId(userId)
                .eventType(type)
                .operation(op)
                .entityId(entityId)
                .build();
        eventStorage.save(event);
        log.debug("Event logged: userId={}, type={}, op={}, entityId={}", userId, type, op, entityId);
    }
}



