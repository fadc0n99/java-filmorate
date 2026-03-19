package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.stream.Collectors;

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
    public void removeLike(long filmId, long userId) {
        Film film = films.get(filmId);
        if (film == null) {
            log.warn("Film with ID {} not found", filmId);
            return;
        }
        Set<Long> likes = film.getLikedUsersFilms();
        if (likes != null) {
            likes.remove(userId);
        }
    }

    @Override
    public boolean isLikeExists(long filmId, long userId) {
        Film film = films.get(filmId);
        return film != null && film.getLikedUsersFilms() != null && film.getLikedUsersFilms().contains(userId);
    }

    @Override
    public List<Film> getPopularFilms(long count) {
        return films.values().stream()
                .sorted((f1, f2) -> Integer.compare(getLikesCount(f2), getLikesCount(f1)))
                .limit(count)
                .collect(Collectors.toList());
    }

    @Override
    public List<Film> findAllByDirector(long directorId, String sortBy) {
        List<Film> directorFilms = films.values().stream()
                .filter(film -> film.getDirectors() != null &&
                        film.getDirectors().stream().anyMatch(d -> d.getId() == directorId))
                .collect(Collectors.toList());

        if ("year".equalsIgnoreCase(sortBy)) {
            directorFilms.sort(Comparator.comparing(Film::getReleaseDate));
        } else if ("likes".equalsIgnoreCase(sortBy)) {
            directorFilms.sort((f1, f2) -> Integer.compare(getLikesCount(f2), getLikesCount(f1)));
        }
        return directorFilms;
    }

    private int getLikesCount(Film film) {
        return film.getLikedUsersFilms() != null ? film.getLikedUsersFilms().size() : 0;
    }

    private Long generateId() {
        long currentId = films.keySet().stream().mapToLong(id -> id).max().orElse(0);
        return ++currentId;
    }
}