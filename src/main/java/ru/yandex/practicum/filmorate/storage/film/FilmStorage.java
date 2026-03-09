package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.Set;

public interface FilmStorage {

    Collection<Film> getAll();

    Film save(Film newFilm);

    Film update(Film newFilm);

    Film getFilmById(long id);

    void clearAll();

    boolean isExistById(long filmId);

    void addLike(long filmId, long userId);

    boolean isLikeExists(long filmId, long userId);

    void removeLike(long filmId, long userId);

    Set<Long> getLikes(Long id);
}
