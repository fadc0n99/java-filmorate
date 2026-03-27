package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.ErrorMessages;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.time.LocalDateTime;
import java.util.*;

@Repository("filmDbStorage")
public class FilmDbStorage extends BaseDbStorage<Film> implements FilmStorage {

    /**
     * Film queries
     */
    private static final String FIND_ALL_FILMS = """
            SELECT f.*, m.*
            FROM films f
            LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_id
            """;

    private static final String FIND_FILM_BY_ID = """
            SELECT f.*, m.*
            FROM films f
            LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_id
            WHERE f.id = ?
            """;

    private static final String FIND_FILMS_BY_IDS = """
            SELECT f.*, m.*
            FROM films f
            LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_id
            WHERE f.id in (:filmIds)
            """;

    private static final String INSERT_FILM = """
            INSERT INTO films (name, description, release_date, duration, mpa_rating_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_FILM = """
            UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ?
            WHERE id = ?
            """;

    private static final String DELETE_FILM = "DELETE FROM films WHERE id = ?";

    private static final String FIND_BY_DIRECTOR_YEAR = """
            SELECT f.*, m.*
            FROM films f
            LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_id
            JOIN film_directors fd ON f.id = fd.film_id
            WHERE fd.director_id = ?
            ORDER BY f.release_date
            """;

    private static final String FIND_BY_DIRECTOR_LIKES = """
            SELECT f.*, m.*
            FROM films f
            LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_id
            JOIN film_directors fd ON f.id = fd.film_id
            LEFT JOIN film_likes fl ON f.id = fl.film_id
            WHERE fd.director_id = ?
            GROUP BY f.id, m.mpa_id, m.mpa_name, m.description
            ORDER BY COUNT(fl.user_id) DESC
            """;

    private static final String EXISTS_FILM_BY_ID = "SELECT EXISTS(SELECT 1 FROM films WHERE id = ?)";

    private static final String FIND_POPULAR_FILMS = """
            WITH film_likes_stats AS (
            SELECT film_id, COUNT(*) AS likes_count
            FROM film_likes
            GROUP BY film_id)
            SELECT f.*, m.*
            FROM films f
            LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_id
            LEFT JOIN film_likes_stats fls ON f.id = fls.film_id
            WHERE (? IS NULL OR EXTRACT(YEAR FROM f.release_date) = ?)
            AND (? IS NULL OR f.id IN (SELECT film_id FROM film_genres WHERE genre_id = ?))
            ORDER BY COALESCE(fls.likes_count, 0) DESC
            LIMIT ?
            """;

    private static final String GET_COMMON_FILMS = """
            SELECT f.*, m.*
            FROM Films f
            LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_id
            JOIN film_likes uf1 ON f.id = uf1.film_id AND uf1.user_id = ?
            JOIN film_likes uf2 ON f.id = uf2.film_id AND uf2.user_id = ?
            JOIN (
                SELECT film_id, COUNT(*) AS likes_count
                FROM film_likes
                GROUP BY film_id
            ) AS film_stats ON f.id = film_stats.film_id
            ORDER BY film_stats.likes_count DESC
            """;

    /**
     * Like queries
     */
    private static final String INSERT_LIKE = """
            INSERT INTO film_likes (film_id, user_id, liked_at)
            VALUES (?, ?, ?)
            """;

    private static final String EXISTS_USER_LIKE = """
            SELECT EXISTS(
                SELECT 1
                FROM film_likes
                WHERE film_id = ? AND user_id = ?
            )
            """;

    private static final String DELETE_LIKE = """
            DELETE FROM film_likes
            WHERE film_id = ? AND user_id = ?
            """;

    private static final String USER_LIKES = "SELECT film_id, user_id FROM FILM_LIKES";
    private static final String USER_LIKES_BY_ID = "SELECT film_id FROM FILM_LIKES WHERE user_id = ?";

    /**
     * Genre and director queries
     */
    private static final String DELETE_FILM_GENRES = "DELETE FROM film_genres WHERE film_id = ?";
    private static final String INSERT_FILM_GENRE = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
    private static final String DELETE_FILM_DIRECTORS = "DELETE FROM film_directors WHERE film_id = ?";
    private static final String INSERT_FILM_DIRECTOR = "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)";

    private static final String FIND_GENRES_BY_FILM_ID =
            "SELECT g.* FROM genres g JOIN film_genres fg ON g.id = fg.genre_id WHERE fg.film_id = ?";

    private static final String FIND_DIRECTORS_BY_FILM_ID =
            "SELECT d.* FROM directors d JOIN film_directors fd ON d.director_id = fd.director_id WHERE fd.film_id = ?";

    private static final String FIND_GENRES_BY_FILM_IDS =
            "SELECT fg.film_id, g.* FROM genres g JOIN film_genres fg ON g.id = fg.genre_id WHERE fg.film_id IN (:ids)";

    private static final String FIND_DIRECTORS_BY_FILM_IDS =
            "SELECT fd.film_id, d.* FROM directors d JOIN film_directors fd ON d.director_id = fd.director_id WHERE fd.film_id IN (:ids)";

    private final RowMapper<Director> directorRowMapper;
    private final RowMapper<Genre> genreRowMapper;

    public FilmDbStorage(JdbcTemplate jdbc,
                         RowMapper<Film> rowMapper,
                         RowMapper<Director> directorRowMapper,
                         RowMapper<Genre> genreRowMapper,
                         NamedParameterJdbcTemplate namedJdbc) {
        super(jdbc, namedJdbc, rowMapper);
        this.directorRowMapper = directorRowMapper;
        this.genreRowMapper = genreRowMapper;
    }

    @Override
    public List<Film> findAll() {
        List<Film> films = findMany(FIND_ALL_FILMS);
        enrichFilmsData(films);
        return films;
    }

    @Override
    public Optional<Film> findFilmById(long id) {
        Optional<Film> film = findOne(FIND_FILM_BY_ID, id);
        film.ifPresent(this::enrichFilmData);
        return film;
    }

    @Override
    public Film save(Film newFilm) {
        long id = insert(INSERT_FILM,
                newFilm.getName(), newFilm.getDescription(), newFilm.getReleaseDate(),
                newFilm.getDuration(), newFilm.getMpa().getId(), LocalDateTime.now());
        newFilm.setId(id);
        updateRelations(newFilm);
        return newFilm;
    }

    @Override
    public Film update(Film film) {
        update(UPDATE_FILM, film.getName(), film.getDescription(), film.getReleaseDate(),
                film.getDuration(), film.getMpa().getId(), film.getId());
        updateRelations(film);
        enrichFilmData(film);

        return findFilmById(film.getId())
                .orElseThrow(() -> new NotFoundException("Film not found after update"));
    }

    @Override
    public List<Film> findAllByDirector(long directorId, String sortBy) {
        String sql = "year".equals(sortBy) ? FIND_BY_DIRECTOR_YEAR : FIND_BY_DIRECTOR_LIKES;
        List<Film> films = findMany(sql, directorId);
        enrichFilmsData(films);
        return films;
    }

    @Override
    public List<Film> getPopularFilms(Integer count, Integer genreId, Integer year) {
        List<Film> films = findMany(FIND_POPULAR_FILMS, year, year, genreId, genreId, count);
        enrichFilmsData(films);
        return films;
    }

    @Override
    public void addLike(long filmId, long userId) {
        insert(INSERT_LIKE, filmId, userId, LocalDateTime.now());
    }

    @Override
    public void removeLike(long filmId, long userId) {
        delete(DELETE_LIKE, filmId, userId);
    }

    @Override
    public boolean isLikeExists(long filmId, long userId) {
        return exists(EXISTS_USER_LIKE, filmId, userId);
    }

    @Override
    public boolean isExistById(long id) {
        return exists(EXISTS_FILM_BY_ID, id);
    }

    @Override
    public void delete(Long id) {
        if (!delete(DELETE_FILM, id)) {
            throw new NotFoundException(ErrorMessages.filmNotFound(id));
        }
    }

    @Override
    public List<Film> getCommonFilms(Long userId, Long friendId) {
        List<Film> popularFilms = findMany(GET_COMMON_FILMS, userId, friendId);
        enrichFilmsData(popularFilms);
        return popularFilms;
    }

    @Override
    public List<Film> findFilmsByIds(List<Long> filmIds) {
        MapSqlParameterSource params = new MapSqlParameterSource("filmIds", filmIds);
        List<Film> films = findMany(FIND_FILMS_BY_IDS, params);
        enrichFilmsData(films);
        return films;
    }

    @Override
    public List<Long> findUserLikedFilmIds(long userId) {
        return findList(USER_LIKES_BY_ID, Long.class, userId);
    }

    @Override
    public Map<Long, List<Long>> findAllUsersLikedFilmIds() {
        return jdbc.query(USER_LIKES, rs -> {
            Map<Long, List<Long>> result = new HashMap<>();
            while (rs.next()) {
                result.computeIfAbsent(
                        rs.getLong("user_id"),
                        v -> new ArrayList<>()
                ).add(rs.getLong("film_id"));
            }
            return result;
        });
    }

    public List<Film> search(String query, String by) {
        String searchPattern = "%" + query.toLowerCase() + "%";
        StringBuilder sql = new StringBuilder(
                "SELECT f.*, m.*, COUNT(fl.user_id) AS rate " +
                        "FROM films f " +
                        "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_id " +
                        "LEFT JOIN film_directors fd ON f.id = fd.film_id " +
                        "LEFT JOIN directors d ON fd.director_id = d.director_id " +
                        "LEFT JOIN film_likes fl ON f.id = fl.film_id "
        );
        List<Object> params = new ArrayList<>();
        if (by.contains("director") && by.contains("title")) {
            sql.append("WHERE LOWER(f.name) LIKE ? OR LOWER(d.director_name) LIKE ? ");
            params.add(searchPattern);
            params.add(searchPattern);
        } else if (by.contains("director")) {
            sql.append("WHERE LOWER(d.director_name) LIKE ? ");
            params.add(searchPattern);
        } else {
            sql.append("WHERE LOWER(f.name) LIKE ? ");
            params.add(searchPattern);
        }
        sql.append("GROUP BY f.id, m.mpa_id, m.mpa_name, m.description ORDER BY rate DESC");
        List<Film> films = findMany(sql.toString(), params.toArray());
        if (!films.isEmpty()) {
            enrichFilmsData(films);
        }
        return films;
    }

    private void enrichFilmData(Film film) {
        List<Genre> genres = findMany(FIND_GENRES_BY_FILM_ID, genreRowMapper, film.getId());
        List<Director> directors = findMany(FIND_DIRECTORS_BY_FILM_ID, directorRowMapper, film.getId());

        film.setGenres(genres);
        film.setDirectors(directors);
    }

    private void enrichFilmsData(List<Film> films) {
        if (films.isEmpty()) return;
        List<Long> ids = films.stream().map(Film::getId).toList();
        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);

        Map<Long, List<Genre>> genresMap = namedJdbc.query(FIND_GENRES_BY_FILM_IDS, params, rs -> {
            Map<Long, List<Genre>> result = new HashMap<>();
            while (rs.next()) {
                result.computeIfAbsent(
                        rs.getLong("film_id"),
                        k -> new ArrayList<>()).add(genreRowMapper.mapRow(rs, rs.getRow())
                );
            }
            return result;
        });

        Map<Long, List<Director>> directorsMap = namedJdbc.query(FIND_DIRECTORS_BY_FILM_IDS, params, rs -> {
            Map<Long, List<Director>> result = new HashMap<>();
            while (rs.next()) {
                Director d = directorRowMapper.mapRow(rs, rs.getRow());
                result.computeIfAbsent(rs.getLong("film_id"), k -> new ArrayList<>()).add(d);
            }
            return result;
        });

        films.forEach(f -> {
            f.setGenres(genresMap.getOrDefault(f.getId(), Collections.emptyList()));
            f.setDirectors(directorsMap.getOrDefault(f.getId(), new ArrayList<>()));
        });
    }

    private void updateRelations(Film film) {
        updateWithoutCheck(DELETE_FILM_GENRES, film.getId());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            List<Object[]> batchArgs = film.getGenres().stream()
                    .distinct()
                    .map(genre -> new Object[]{film.getId(), genre.getId()})
                    .toList();
            batchUpdate(INSERT_FILM_GENRE, batchArgs);
        }

        updateWithoutCheck(DELETE_FILM_DIRECTORS, film.getId());
        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            List<Object[]> batchArgs = film.getDirectors().stream()
                    .map(d -> new Object[]{film.getId(), d.getId()})
                    .toList();
            batchUpdate(INSERT_FILM_DIRECTOR, batchArgs);
        }
    }
}