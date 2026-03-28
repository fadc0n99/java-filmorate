package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ErrorMessages;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreStorage genreStorage;

    public List<Genre> findAllGenres() {
        return genreStorage.findAll();
    }

    public Genre findGenreById(long id) {
        return genreStorage.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.genreNotFound(id)));
    }

    public List<Genre> findGenresByIds(List<Long> genreIds) {
        return genreStorage.findGenresByIds(genreIds);
    }

    public Map<Long, List<Genre>> getGenresByFilmIds(List<Long> filmIds) {
        return genreStorage.findGenresByFilmIds(filmIds);
    }
}
