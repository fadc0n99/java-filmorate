package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.film.CreateFilmDto;
import ru.yandex.practicum.filmorate.dto.film.FilmDto;
import ru.yandex.practicum.filmorate.dto.film.UpdateFilmDto;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.List;

@RestController
@RequestMapping("/films")
@RequiredArgsConstructor
@Slf4j
public class FilmController {

    private final FilmService filmService;

    @GetMapping
    public ResponseEntity<List<FilmDto>> handleFindAll() {
        log.debug("Request received: GET /films - retrieving all films");

        List<FilmDto> films = filmService.findAllFilms();

        log.info("Retrieved {} films successfully", films.size());
        return new ResponseEntity<>(films, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<FilmDto> handleCreateFilm(@Valid @RequestBody CreateFilmDto createFilmDto) {
        log.debug("Request received: POST /films - creating new film: {}", createFilmDto);

        FilmDto createdFilm = filmService.createFilm(createFilmDto);

        log.info("Film created successfully. ID: {}, Name: {}", createdFilm.getId(), createdFilm.getName());
        return new ResponseEntity<>(createdFilm, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<FilmDto> handleUpdateFilm(@Valid @RequestBody UpdateFilmDto updateFilmDto) {
        log.debug("Request received: PUT /films - updating film. ID: {}, Name: {}",
                updateFilmDto.getId(), updateFilmDto.getName());

        FilmDto updatedFilm = filmService.updateFilm(updateFilmDto);

        log.info("Film updated successfully. ID: {}, Name: {}", updatedFilm.getId(), updatedFilm.getName());
        return new ResponseEntity<>(updatedFilm, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FilmDto> handleGetFilmById(@PathVariable long id) {
        log.debug("Request received: GET /films/{} - retrieving film by ID", id);

        FilmDto film = filmService.getFilm(id);

        log.info("Film retrieved successfully. ID: {}, Name: {}", film.getId(), film.getName());
        return new ResponseEntity<>(film, HttpStatus.OK);
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
    public ResponseEntity<List<FilmDto>> handlePopularFilms(
            @RequestParam(defaultValue = "10") @Positive Integer count) {
        log.debug("Request received: GET /films/popular - retrieving popular films");

        List<FilmDto> popularFilms = filmService.getPopularFilms(count);

        log.info("Retrieved {} popular films (limit={})", popularFilms.size(), count);

        return new ResponseEntity<>(popularFilms, HttpStatus.OK);
    }

    @GetMapping("/director/{directorId}")
    public ResponseEntity<List<FilmDto>> handleGetFilmsByDirector(
            @PathVariable Integer directorId,
            @RequestParam String sortBy) {
        log.debug("Request received: GET /films/director/{} - sorting by {}", directorId, sortBy);

        if (!sortBy.equals("year") && !sortBy.equals("likes")) {
            throw new IllegalArgumentException("Сортировка возможна только по year или likes");
        }

        List<FilmDto> films = filmService.getFilmsByDirector(directorId, sortBy);

        log.info("Retrieved {} films for director ID: {}", films.size(), directorId);
        return new ResponseEntity<>(films, HttpStatus.OK);
    }

}
