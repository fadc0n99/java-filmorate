package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Long, Film> films;

    @Override
    public List<Film> findAll() {
        return films.values().stream().toList();
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
    public Optional<Film> findFilmById(long id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public boolean isExistById(long filmId) {
        return films.containsKey(filmId);
    }

    @Override
    public void removeLike(long filmId, long userId) {
        Film film = films.get(filmId);
        if (film == null) {
            log.warn("Film with ID {} not found, cannot remove like from user {}", filmId, userId);
            return;
        }

        Set<Long> likes = film.getLikedUsersFilms();
        if (likes != null) {
            likes.remove(userId);
        }
    }

    public Set<Long> getLikes(Long id) {
        Film film = films.get(id);
        if (film == null) {
            return Collections.emptySet();
        }

        Set<Long> likes = film.getLikedUsersFilms();
        return likes != null ? likes : Collections.emptySet();
    }

    @Override
    public List<Film> getPopularFilms(long count) {
        return films.values().stream()
                .sorted(Comparator.comparingInt((Film film) -> getLikes(film.getId()).size()).reversed())
                .limit(count)
                .toList();
    }

    @Override
    public List<Film> findFilmsByIds(List<Long> filmIds) {
        return List.of();
    }

    @Override
    public List<Long> findUserLikedFilmIds(long userId) {
        return List.of();
    }

    @Override
    public Map<Long, List<Long>> findAllUsersLikedFilmIds() {
        return Map.of();
    }

    @Override
    public void addLike(long filmId, long userId) {
        Film film = films.get(filmId);

        if (film == null) {
            throw new NotFoundException("Film with ID " + filmId + " not found");
        }

        Set<Long> likes = film.getLikedUsersFilms();
        if (likes == null) {
            likes = new HashSet<>();
            film.setLikedUsersFilms(likes);
        }
        likes.add(userId);
    }

    @Override
    public boolean isLikeExists(long filmId, long userId) {
        Film film = films.get(filmId);
        if (film == null) {
            return false;
        }

        Set<Long> likes = film.getLikedUsersFilms();
        return likes != null && likes.contains(userId);
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
