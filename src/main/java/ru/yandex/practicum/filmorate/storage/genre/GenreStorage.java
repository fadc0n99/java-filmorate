package ru.yandex.practicum.filmorate.storage.genre;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GenreStorage {
    List<Genre> findAll();

    Optional<Genre> findById(long id);

    List<Genre> findGenresByIds(List<Long> genreIds);

    List<Long> findGenreIdsByIds(List<Long> genreIds);

    Map<Long, List<Genre>> findGenresByFilmIds(List<Long> filmIds);

    boolean isExistById(long id);

    boolean isExistByIds(List<Long> ids);
}