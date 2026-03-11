package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.dto.GenreId;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FilmStorage {

    List<Film> findAll();

    Film save(Film newFilm);

    Film update(Film newFilm);

    Optional<Film> findFilmById(long id);

    void clearAll();

    boolean isExistById(long filmId);

    void addLike(long filmId, long userId);

    boolean isLikeExists(long filmId, long userId);

    void removeLike(long filmId, long userId);

    Set<Long> getLikes(Long id);

    boolean isMpaExist(Long mpaId);

    boolean isGenresExist(List<GenreId> genresId);

    List<Film> getPopularFilms(long count);
}
