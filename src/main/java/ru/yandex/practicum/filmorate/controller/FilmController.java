package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
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
@Validated
public class FilmController {

    private final FilmService filmService;

    @GetMapping
    public List<FilmDto> handleFindAll() {
        log.debug("Request received: GET /films - retrieving all films");
        return filmService.findAllFilms();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FilmDto handleCreateFilm(@Valid @RequestBody CreateFilmDto createFilmDto) {
        log.debug("Request received: POST /films - creating new film: {}", createFilmDto);
        return filmService.createFilm(createFilmDto);
    }

    @PutMapping
    public FilmDto handleUpdateFilm(@Valid @RequestBody UpdateFilmDto updateFilmDto) {
        log.debug("Request received: PUT /films - updating film ID: {}", updateFilmDto.getId());
        return filmService.updateFilm(updateFilmDto);
    }

    @GetMapping("/{id}")
    public FilmDto handleGetFilmById(@PathVariable long id) {
        log.debug("Request received: GET /films/{} - retrieving film by ID", id);
        return filmService.getFilm(id);
    }

    //  эндпоинт для удаления фильма
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handleDeleteFilm(@PathVariable long id) {
        log.debug("Request received: DELETE /films/{} - deleting film", id);
        filmService.deleteFilm(id);
        log.info("Film {} deleted successfully", id);
    }

    @PutMapping("/{id}/like/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public void handleAddFilmLike(@PathVariable(value = "id") long filmId, @PathVariable long userId) {
        log.debug("Request received: PUT /films/{}/like/{} - adding like", filmId, userId);

        filmService.addFilmLike(filmId, userId);

        log.info("Like added successfully. Film: {}, User: {}", filmId, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handleRemoveFilmLike(@PathVariable(value = "id") long filmId, @PathVariable long userId) {
        log.debug("Request received: DELETE /films/{}/like/{} - removing like", filmId, userId);

        filmService.removeFilmLike(filmId, userId);

        log.info("Like removed successfully. Film: {}, User: {}", filmId, userId);
    }

    @GetMapping("/popular")
    public List<FilmDto> handlePopularFilms(
            @RequestParam(defaultValue = "10") @Positive Integer count,
            @RequestParam(required = false) Integer genreId,
            @RequestParam(required = false) Integer year) {
        log.info("Request received: GET /films/popular - retrieving popular films - Retrieved  popular films (limit={}, genreId={}, year={})",
                count, genreId, year);
        return filmService.getPopularFilms(count, genreId, year);
    }

    @GetMapping("/director/{directorId}")
    public List<FilmDto> handleGetFilmsByDirector(
            @PathVariable @NotNull Integer directorId,
            @RequestParam @Pattern(regexp = "year|likes",
                    message = "Сортировка возможна только по 'year' или 'likes'") String sortBy) {

        log.debug("Request received: GET /films/director/{} - sorting by {}", directorId, sortBy);
        return filmService.getFilmsByDirector(directorId, sortBy);
    }

    @GetMapping("/common")
    public List<FilmDto> getCommonFilms(
            @RequestParam Long userId,
            @RequestParam Long friendId) {
        log.debug("Request received: GET /films/common?userId={userId}&friendId={friendId} - Returns a list of movies sorted by popularity");
        return filmService.getCommonFilmsSortedByPopularity(userId, friendId);
    }

    @GetMapping("/search")
    public List<FilmDto> search(@RequestParam String query, @RequestParam String by) {
        log.info("Received search request: query='{}', by='{}'", query, by);
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return filmService.searchFilms(query, by);
    }
}