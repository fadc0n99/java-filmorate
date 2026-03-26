package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface FilmStorage {

    List<Film> findAll();

    Film save(Film newFilm);

    Film update(Film newFilm);

    Optional<Film> findFilmById(long id);

    boolean isExistById(long filmId);

    void addLike(long filmId, long userId);

    boolean isLikeExists(long filmId, long userId);

    void removeLike(long filmId, long userId);

    List<Film> getPopularFilms(Integer count, Integer genreId, Integer year);

    List<Film> getCommonFilms(Long userId, Long friendId);

    List<Film> findFilmsByIds(List<Long> filmIds);

    List<Long> findUserLikedFilmIds(long userId);

    public Map<Long, List<Long>> findAllUsersLikedFilmIds();

    List<Film> findAllByDirector(long directorId, String sortBy);

    void delete(Long id);

}
