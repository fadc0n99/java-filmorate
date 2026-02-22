package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserService userService;

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private static final String ERROR_INVALID_ID_MESSAGE = "Invalid film ID: must be greater than 0";
    private static final String ERROR_FILM_NOT_FOUND_MESSAGE = "Film with ID %d not found";
    private static final String ERROR_RELEASE_DATE_MESSAGE =
            String.format("Release date must be after %s", MIN_RELEASE_DATE);

    public Collection<Film> findAllFilms() {
        log.debug("Retrieving all films from storage");

        return filmStorage.getAll();
    }

    public Film createFilm(Film newFilm) {
        validateReleaseDate(newFilm);

        return filmStorage.save(newFilm);
    }

    public Film updateFilm(Film newFilm) {
        requireValidFilm(newFilm.getId());
        validateReleaseDate(newFilm);

        log.debug("Starting update film. ID: {}, Name: {}", newFilm.getId(), newFilm.getName());

        Film updatedFilm = filmStorage.update(newFilm);
        log.debug("Film update completed. ID: {}, Name: {}", updatedFilm.getId(), updatedFilm.getName());

        return updatedFilm;
    }

    public Film getFilm(long id) {
        requireValidFilm(id);

        log.debug("Retrieving film by ID: {}", id);
        Film film = filmStorage.getFilmById(id);
        log.trace("Retrieved film details: {}", film);

        return film;
    }

    public void addFilmLike(long filmId, long userId) {
        requireValidFilm(filmId);
        userService.requireValidUser(userId);

        log.debug("Adding like. Film: {}, User: {}", filmId, userId);

        if(isLikeExists(filmId, userId)) {
            log.warn("User {} already liked film {}", userId, filmId);
            throw new ValidationException("User has already liked this film");
        }

        log.info("Like added successfully. Film: {}, User: {}", filmId, userId);
        filmStorage.addLike(filmId, userId);
    }

    public void removeFilmLike(long filmId, long userId) {
        requireValidFilm(filmId);
        userService.requireValidUser(userId);

        log.debug("Removing like. Film: {}, User: {}", filmId, userId);

        if(!isLikeExists(filmId, userId)) {
            log.warn("Cannot remove non-existent like. Film: {}, User: {}", filmId, userId);
            throw new ValidationException("User hasn't liked this film");
        }

        filmStorage.removeLike(filmId, userId);
        log.info("Like removed successfully. Film: {}, User: {}", filmId, userId);
    }

    public void requireValidFilm(long filmId) {
        if (filmId <= 0) {
            throw new ValidationException(ERROR_INVALID_ID_MESSAGE);
        }
        if (!isFilmExists(filmId)) {
            throw new NotFoundException(String.format(ERROR_FILM_NOT_FOUND_MESSAGE, filmId));
        }
    }

    public boolean isFilmExists(long filmId) {
        return filmStorage.isExistById(filmId);
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            throw new ValidationException(ERROR_RELEASE_DATE_MESSAGE);
        }
    }

    public boolean isLikeExists(long filmId, long userId) {
        return filmStorage.isLikeExists(filmId, userId);
    }

    public List<Film> getPopularFilms(long count) {
        log.debug("Getting top {} popular films", count);

        if (count <= 0) {
            throw new ValidationException("Count must be positive");
        }

        List<Film> allFilms = new ArrayList<>(filmStorage.getAll());

        return allFilms.stream()
                .sorted(Comparator.comparingInt((Film film) -> filmStorage.getLikes(film.getId()).size()).reversed())
                .limit(count)
                .toList();
    }
}
