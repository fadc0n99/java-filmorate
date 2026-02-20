package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class FilmService {

    private final Map<Long, Film> films = new HashMap<>();

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    public Collection<Film> findAllFilms() {
        return films.values();
    }

    public Film createFilm(Film newFilm) {
        newFilm.setId(generateId());

        if (isInvalidReleaseDate(newFilm)) {
            log.warn("Create film error. Incorrect release date: {}", newFilm.getReleaseDate());
            throw new ValidationException("Release date must be after " + MIN_RELEASE_DATE);
        }

        films.put(newFilm.getId(), newFilm);

        return newFilm;
    }

    public Film updateFilm(Film newFilm) {
        if (newFilm.getId() == null) {
            log.warn("Film not be update: missing ID. Request data: {}", newFilm);
            throw new ValidationException("ID not be null or empty");
        }
        if (isInvalidReleaseDate(newFilm)) {
            log.warn("Update film error. Incorrect release date: {}", newFilm.getReleaseDate());
            throw new ValidationException("Release date must be after " + MIN_RELEASE_DATE);
        }

        if (films.containsKey(newFilm.getId())) {
            log.debug("Starting update film. ID: {}, Name: {}", newFilm.getId(), newFilm.getName());
            Film oldFilm = films.get(newFilm.getId());

            log.trace("Current film data: {}", oldFilm);

            oldFilm.setDescription(newFilm.getDescription());
            oldFilm.setName(newFilm.getName());
            oldFilm.setReleaseDate(newFilm.getReleaseDate());
            oldFilm.setDuration(newFilm.getDuration());

            log.info("User updated. ID: {}", oldFilm.getId());
            log.trace("Updated user data: {}", oldFilm);

            return oldFilm;
        }

        log.warn("Film with ID {} not found, not be update", newFilm.getId());
        throw new NotFoundException("Film with ID " + newFilm.getId() + " not found");
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

    private boolean isInvalidReleaseDate(Film newFilm) {
        return newFilm.getReleaseDate().isBefore(MIN_RELEASE_DATE);
    }

}
