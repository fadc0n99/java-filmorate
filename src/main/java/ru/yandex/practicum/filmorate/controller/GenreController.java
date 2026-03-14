package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.service.GenreService;

import java.util.List;

@RestController
@RequestMapping("/genres")
@RequiredArgsConstructor
@Slf4j
public class GenreController {

    private final GenreService genreService;

    @GetMapping
    public ResponseEntity<List<Genre>> getGenres() {
        log.debug("Request received: GET /genres - retrieving all genres");

        List<Genre> genres = genreService.findAllGenres();

        log.info("Retrieved {} genres successfully", genres.size());
        return ResponseEntity.ok(genres);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Genre> getGenreById(@PathVariable long id) {
        log.debug("Request received: GET /genres/{} - retrieving genre by ID", id);

        Genre genre = genreService.findGenreById(id);

        log.info("Genre retrieved successfully. ID: {}, Name: {}",
                genre.getId(), genre.getName());
        return ResponseEntity.ok(genre);
    }

}
