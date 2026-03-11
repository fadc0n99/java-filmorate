package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GenreService {

    private static final String GENRE_NOT_FOUND_MESSAGE = "Genre with ID %d not found";

    private final GenreStorage genreStorage;

    public List<Genre> findAllGenres() {
        return genreStorage.findAll();
    }

    public Genre findGenreById(long id) {
        return genreStorage.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format(GENRE_NOT_FOUND_MESSAGE, id)));
    }

    public Set<Genre> findGenresByIds(Set<Long> genreIds) {
        return new HashSet<>(genreStorage.findByIdIn(genreIds));
    }
}
