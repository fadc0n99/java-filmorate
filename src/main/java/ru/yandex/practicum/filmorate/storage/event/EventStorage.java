package ru.yandex.practicum.filmorate.storage.event;

import ru.yandex.practicum.filmorate.model.Event;
import java.util.List;

public interface EventStorage {

    Event save(Event event);

    List<Event> findFeedByUserId(Long userId, Integer count);
}

