package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.Collection;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {

    private final FilmService filmService;

    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @GetMapping
    public ResponseEntity<Collection<Film>> findAll() {
        Collection<Film> films = filmService.findAllFilms();

        return new ResponseEntity<>(films, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Film> create(@Valid @RequestBody Film newFilm) {
        log.debug("Creating film: {}", newFilm);

        Film createdFilm = filmService.createFilm(newFilm);

        log.info("Film created. ID: {}, Name: {}", createdFilm.getId(), createdFilm.getName());


        return new ResponseEntity<>(createdFilm, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<Film> update(@Valid @RequestBody Film newFilm) {
        log.debug("Updating film. ID: {}", newFilm.getId());

        Film updatedFilm = filmService.updateFilm(newFilm);

        log.info("Film updated. ID: {}", updatedFilm.getId());
        log.trace("Updated film data: {}", updatedFilm);

        return new ResponseEntity<>(updatedFilm, HttpStatus.OK);
    }
}
