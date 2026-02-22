package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Long, Film> films;
    private final Map<Long, Set<Long>> likedUsersFilms;

    @Override
    public Collection<Film> getAll() {
        return films.values();
    }

    @Override
    public Film save(Film newFilm) {
        long newId = generateId();
        newFilm.setId(newId);

        films.put(newId, newFilm);

        return newFilm;
    }

    @Override
    public Film update(Film newFilm) {
        films.put(newFilm.getId(), newFilm);

        return newFilm;
    }

    @Override
    public Film getFilmById(long id) {
        return films.get(id);
    }

    @Override
    public boolean isExistById(long filmId) {
        return films.containsKey(filmId);
    }

    @Override
    public void removeLike(long filmId, long userId) {
        Set<Long> likes = likedUsersFilms.get(filmId);
        if (likes != null) {
            likes.remove(userId);
            if (likes.isEmpty()) {
                likedUsersFilms.remove(filmId);
            }
        }
    }

    @Override
    public Set<Long> getLikes(Long id) {
        return likedUsersFilms.getOrDefault(id, new HashSet<>());
    }

    @Override
    public void addLike(long filmId, long userId) {
        likedUsersFilms.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
    }

    @Override
    public boolean isLikeExists(long filmId, long userId) {
        Set<Long> likes = likedUsersFilms.get(filmId);
        return likes != null && likes.contains(userId);
    }

    @Override
    public void clearAll() {
        films.clear();
    }

    private Long generateId() {
        long currentId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);

        log.debug("Generated new ID for Film. New ID: {}", currentId + 1);

        return ++currentId;
    }


}
