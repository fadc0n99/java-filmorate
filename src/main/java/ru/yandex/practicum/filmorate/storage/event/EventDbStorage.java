package ru.yandex.practicum.filmorate.storage.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.util.List;

@Repository
@Slf4j
public class EventDbStorage extends BaseDbStorage<Event> implements EventStorage {

    private static final String INSERT_EVENT = """
            INSERT INTO events (timestamp, user_id, event_type, operation, entity_id)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String EVENTS_BY_USER_ID = """
            SELECT event_id, timestamp, user_id, event_type, operation, entity_id
            FROM events
            WHERE user_id = ?
            ORDER BY timestamp ASC
            """;

    public EventDbStorage(JdbcTemplate jdbc,
                          RowMapper<Event> eventRowMapper,
                          NamedParameterJdbcTemplate namedJdbc) {
        super(jdbc, namedJdbc, eventRowMapper);
    }

    @Override
    public Event save(Event event) {
        log.debug("Saving event: userId={}, type={}, operation={}, entityId={}",
                event.getUserId(), event.getEventType(), event.getOperation(), event.getEntityId());

        long eventId = insert(
                INSERT_EVENT,
                event.getTimestamp(),
                event.getUserId(),
                event.getEventType().getValue(),
                event.getOperation().getValue(),
                event.getEntityId()
        );

        event.setEventId(eventId);
        log.debug("Event saved with id={}", event.getEventId());

        return event;
    }

    @Override
    public List<Event> findFeedByUserId(Long userId) {
        log.debug("Fetching feed for user {})", userId);

        return findMany(EVENTS_BY_USER_ID, userId);
    }
}

