package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;

import java.util.*;

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

    public List<Genre> findGenresByIds(List<Long> genreIds) {
        return genreStorage.findGenresByIds(genreIds);
    }

    public boolean isGenresExist(List<Long> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return true;
        }

        Set<Long> uniqueIds = new HashSet<>(genreIds);
        List<Long> foundIds = genreStorage.findGenreIdsByIds(genreIds);

        return foundIds.size() == uniqueIds.size();
    }
}
