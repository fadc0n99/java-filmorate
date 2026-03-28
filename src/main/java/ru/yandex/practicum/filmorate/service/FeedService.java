package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.event.EventDto;
import ru.yandex.practicum.filmorate.mapper.EventMapper;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
import ru.yandex.practicum.filmorate.utils.ValidationEntityUtils;

import java.util.List;

@Service
@Slf4j
public class FeedService {

    private final EventStorage eventStorage;
    private final ValidationEntityUtils validationEntityUtils;

    @Autowired
    public FeedService(EventStorage eventStorage,
                       ValidationEntityUtils validationEntityUtils) {
        this.eventStorage = eventStorage;
        this.validationEntityUtils = validationEntityUtils;
    }

    public List<EventDto> getUserFeed(Long userId) {
        validationEntityUtils.validateUserExists(userId);

        log.debug("Getting feed for user {}", userId);
        List<Event> events = eventStorage.findFeedByUserId(userId);

        return events.stream()
                .map(EventMapper::toDto)
                .toList();
    }

    public void saveEvent(Long userId, EventType type, Operation op, Long entityId) {
        validationEntityUtils.validateUserExists(userId);

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




