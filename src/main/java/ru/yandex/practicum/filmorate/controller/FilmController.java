package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/films")
@RequiredArgsConstructor
@Slf4j
public class FilmController {

    private final FilmService filmService;
    @Value("${filmorate.popular-films.default-count:10}")
    private int defaultPopularFilmsCount;

    @GetMapping
    public ResponseEntity<Collection<Film>> handleFindAll() {
        log.debug("Request received: GET /films - retrieving all films");

        Collection<Film> films = filmService.findAllFilms();

        log.info("Retrieved {} films successfully", films.size());
        return new ResponseEntity<>(films, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Film> handleCreateFilm(@Valid @RequestBody Film newFilm) {
        log.debug("Request received: POST /films - creating new film: {}", newFilm);

        Film createdFilm = filmService.createFilm(newFilm);

        log.info("Film created successfully. ID: {}, Name: {}", createdFilm.getId(), createdFilm.getName());
        return new ResponseEntity<>(createdFilm, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<Film> handleUpdateFilm(@Valid @RequestBody Film newFilm) {
        log.debug("Request received: PUT /films - updating film. ID: {}, Name: {}",
                newFilm.getId(), newFilm.getName());

        Film updatedFilm = filmService.updateFilm(newFilm);

        log.info("Film updated successfully. ID: {}, Name: {}", updatedFilm.getId(), updatedFilm.getName());
        return new ResponseEntity<>(updatedFilm, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public Film handleGetFilmById(@PathVariable long id) {
        log.debug("Request received: GET /films/{} - retrieving film by ID", id);

        Film film = filmService.getFilm(id);

        log.info("Film retrieved successfully. ID: {}, Name: {}", film.getId(), film.getName());
        return film;
    }

    @PutMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> handleAddFilmLike(
            @PathVariable(value = "id") long filmId,
            @PathVariable long userId) {
        log.debug("Request received: PUT /films/{}/like/{} - adding like", filmId, userId);

        filmService.addFilmLike(filmId, userId);

        log.info("Like added successfully. Film: {}, User: {}", filmId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> handleRemoveFilmLike(
            @PathVariable(value = "id") long filmId,
            @PathVariable long userId
    ) {
        log.debug("Request received: DELETE /films/{}/like/{} - removing like", filmId, userId);

        filmService.removeFilmLike(filmId, userId);

        log.info("Like removed successfully. Film: {}, User: {}", filmId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/popular")
    public ResponseEntity<List<Film>> handlePopularFilms(@RequestParam(required = false) Integer count) {
        log.debug("Request received: GET /films/popular - retrieving popular films");

        int actualCount = count != null ? count : defaultPopularFilmsCount;
        List<Film> popularFilms = filmService.getPopularFilms(actualCount);

        log.info("Retrieved {} popular films (count={})", popularFilms.size(), actualCount);

        return new ResponseEntity<>(popularFilms, HttpStatus.OK);
    }
}
