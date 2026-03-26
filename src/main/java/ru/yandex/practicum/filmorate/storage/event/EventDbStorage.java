package ru.yandex.practicum.filmorate.storage.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@Slf4j
public class EventDbStorage extends BaseDbStorage<Event> implements EventStorage {

    public EventDbStorage(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, new EventRowMapper());
    }

    @Override
    public Event save(Event event) {
        log.debug("Saving event: userId={}, type={}, operation={}, entityId={}",
                event.getUserId(), event.getEventType(), event.getOperation(), event.getEntityId());

        String sql = """
                INSERT INTO events (timestamp, user_id, event_type, operation, entity_id)
                VALUES (?, ?, ?, ?, ?)
                """;

        long eventId = insert(sql,
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
    public List<Event> findFeedByUserId(Long userId, Integer count) {
        log.debug("Fetching feed for user {} (limit={})", userId, count);

        String sql = """
                SELECT event_id, timestamp, user_id, event_type, operation, entity_id
                FROM events
                WHERE user_id = ?
                ORDER BY timestamp ASC
                LIMIT ?
                """;

        return findMany(sql, userId, count);
    }

    private static class EventRowMapper implements RowMapper<Event> {
        @Override
        public Event mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Event.builder()
                    .eventId(rs.getLong("event_id"))
                    .timestamp(rs.getLong("timestamp"))
                    .userId(rs.getLong("user_id"))
                    .eventType(EventType.valueOf(rs.getString("event_type")))
                    .operation(Operation.valueOf(rs.getString("operation")))
                    .entityId(rs.getLong("entity_id"))
                    .build();
        }
    }
}

