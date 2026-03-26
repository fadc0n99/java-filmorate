package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.event.EventDto;
import ru.yandex.practicum.filmorate.exception.ErrorMessages;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

@Service
@Slf4j
public class FeedService {

    private final EventStorage eventStorage;
    private final UserStorage userStorage;

    @Autowired
    public FeedService(EventStorage eventStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage) {
        this.eventStorage = eventStorage;
        this.userStorage = userStorage;
    }

    public List<EventDto> getUserFeed(Long userId) {
        validateUserExists(userId);

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

    public void logEvent(Long userId, EventType type, Operation op, Long entityId) {
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

    private void validateUserExists(long userId) {
        if (!userStorage.isExistById(userId)) {
            throw new NotFoundException(ErrorMessages.userNotFound(userId));
        }
    }
}




