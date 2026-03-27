package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.event.EventDto;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
import ru.yandex.practicum.filmorate.utils.ValidationUtils;

import java.util.List;

@Service
@Slf4j
public class FeedService {

    private final EventStorage eventStorage;
    private final ValidationUtils validationUtils;

    @Autowired
    public FeedService(EventStorage eventStorage,
                       ValidationUtils validationUtils) {
        this.eventStorage = eventStorage;
        this.validationUtils = validationUtils;
    }

    public List<EventDto> getUserFeed(Long userId) {
        validationUtils.validateUserExists(userId);

        log.debug("Getting feed for user {}", userId);
        List<Event> events = eventStorage.findFeedByUserId(userId);

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

    public void saveEvent(Long userId, EventType type, Operation op, Long entityId) {
        validationUtils.validateUserExists(userId);

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




