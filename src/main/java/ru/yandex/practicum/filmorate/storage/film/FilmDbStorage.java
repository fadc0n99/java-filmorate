package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;
import ru.yandex.practicum.filmorate.storage.director.DirectorDbStorage;

import java.time.LocalDateTime;
import java.util.*;

@Repository("filmDbStorage")
public class FilmDbStorage extends BaseDbStorage<Film> implements FilmStorage {

    /**
     * Film queries
     */
    private static final String FIND_ALL_FILMS = "SELECT * FROM films";

    private static final String FIND_FILM_BY_ID = "SELECT * FROM films WHERE id = ?";

    private static final String FIND_FILMS_BY_IDS = "SELECT * FROM films WHERE id in (:filmIds)";

    private static final String INSERT_FILM_QUERY = """
            INSERT INTO films (name, description, release_date, duration, mpa_rating_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_FILM_QUERY = """
            UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ? 
            WHERE id = ?
            """;
    private static final String FIND_BY_DIRECTOR_YEAR = """
            SELECT f.* FROM films f 
            JOIN film_directors fd ON f.id = fd.film_id 
            WHERE fd.director_id = ? ORDER BY f.release_date
            """;
    private static final String FIND_BY_DIRECTOR_LIKES = """
            SELECT f.* FROM films f 
            JOIN film_directors fd ON f.id = fd.film_id 
            LEFT JOIN film_likes fl ON f.id = fl.film_id 
            WHERE fd.director_id = ? 
            GROUP BY f.id ORDER BY COUNT(fl.user_id) DESC
            """;

    private static final String EXISTS_FILM_BY_ID = "SELECT EXISTS(SELECT 1 FROM films WHERE id = ?)";

    private static final String FIND_POPULAR_FILMS = """
        SELECT f.*
        FROM films f
        LEFT JOIN (
            SELECT film_id, COUNT(*) AS likes_count
            FROM film_likes
            GROUP BY film_id
        ) AS film_stats ON f.id = film_stats.film_id
        ORDER BY COALESCE(film_stats.likes_count, 0) DESC
        LIMIT ?
        """;

    private static final String GET_COMMON_FILMS = """
        SELECT f.*
          FROM Films f
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
    private static final String INSERT_LIKE_FILM = """
        INSERT INTO film_likes (film_id, user_id, liked_at)
        VALUES (?, ?, ?)
        """;

    private static final String EXIST_USER_LIKE = """
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

    private final NamedParameterJdbcTemplate namedJdbc;

    public FilmDbStorage(JdbcTemplate jdbc, RowMapper<Film> rowMapper, NamedParameterJdbcTemplate namedJdbc) {
        super(jdbc, rowMapper);
        this.namedJdbc = namedJdbc;
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
        long id = insert(INSERT_FILM_QUERY,
                newFilm.getName(), newFilm.getDescription(), newFilm.getReleaseDate(),
                newFilm.getDuration(), newFilm.getMpaId(), LocalDateTime.now());
        newFilm.setId(id);
        updateRelations(newFilm);
        return newFilm;
    }

    @Override
    public Film update(Film film) {
        update(UPDATE_FILM_QUERY, film.getName(), film.getDescription(), film.getReleaseDate(),
                film.getDuration(), film.getMpaId(), film.getId());
        updateRelations(film);
        enrichFilmData(film);
        return film;
    }

    @Override
    public List<Film> findAllByDirector(long directorId, String sortBy) {
        String sql = "year".equals(sortBy) ? FIND_BY_DIRECTOR_YEAR : FIND_BY_DIRECTOR_LIKES;
        List<Film> films = findMany(sql, directorId);
        enrichFilmsData(films);
        return films;
    }

    @Override
    public List<Film> getPopularFilms(long count) {
        List<Film> films = findMany(FIND_POPULAR_FILMS, (int) count);
        enrichFilmsData(films);
        return films;
    }

    @Override
    public void addLike(long filmId, long userId) {
        jdbc.update(INSERT_LIKE_FILM,
                filmId, userId, LocalDateTime.now());
    }

    @Override
    public void removeLike(long filmId, long userId) {
        delete(DELETE_LIKE, filmId, userId);
    }

    @Override
    public boolean isLikeExists(long filmId, long userId) {
        return isExistOne(EXIST_USER_LIKE,
                filmId, userId);
    }

    @Override
    public boolean isExistById(long id) {
        return isExistOne(EXISTS_FILM_BY_ID, id);
    }

    private void updateRelations(Film film) {
        // Жанры
        jdbc.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        if (film.getGenresIds() != null && !film.getGenresIds().isEmpty()) {
            List<Object[]> batchArgs = film.getGenresIds().stream()
                    .distinct()
                    .map(gId -> new Object[]{film.getId(), gId})
                    .toList();
            batchUpdate("INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)", batchArgs);
        }

        jdbc.update("DELETE FROM film_directors WHERE film_id = ?", film.getId());
        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            List<Object[]> batchArgs = film.getDirectors().stream()
                    .map(d -> new Object[]{film.getId(), d.getId()})
                    .toList();
            batchUpdate("INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)", batchArgs);
        }
    }

    private void enrichFilmData(Film film) {
        film.setGenresIds(jdbc.query("SELECT genre_id FROM film_genres WHERE film_id = ?",
                (rs, rowNum) -> rs.getLong("genre_id"), film.getId()));

        film.setDirectors(jdbc.query(
                "SELECT d.* FROM directors d JOIN film_directors fd ON d.director_id = fd.director_id WHERE fd.film_id = ?",
                DirectorDbStorage.DIRECTOR_MAPPER, film.getId()));
    }

    private void enrichFilmsData(List<Film> films) {
        if (films.isEmpty()) return;
        List<Long> ids = films.stream().map(Film::getId).toList();
        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);

        Map<Long, List<Long>> genresMap = namedJdbc.query(
                "SELECT film_id, genre_id FROM film_genres WHERE film_id IN (:ids)", params, rs -> {
                    Map<Long, List<Long>> result = new HashMap<>();
                    while (rs.next()) {
                        result.computeIfAbsent(rs.getLong("film_id"), k -> new ArrayList<>()).add(rs.getLong("genre_id"));
                    }
                    return result;
                });

        Map<Long, List<Director>> directorsMap = namedJdbc.query(
                "SELECT fd.film_id, d.* FROM directors d JOIN film_directors fd ON d.director_id = fd.director_id WHERE fd.film_id IN (:ids)",
                params, rs -> {
                    Map<Long, List<Director>> result = new HashMap<>();
                    while (rs.next()) {
                        Director d = DirectorDbStorage.DIRECTOR_MAPPER.mapRow(rs, rs.getRow());
                        result.computeIfAbsent(rs.getLong("film_id"), k -> new ArrayList<>()).add(d);
                    }
                    return result;
                });

        films.forEach(f -> {
            f.setGenresIds(genresMap.getOrDefault(f.getId(), Collections.emptyList()));
            f.setDirectors(directorsMap.getOrDefault(f.getId(), new ArrayList<>()));
        });
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

        return namedJdbc.query(FIND_FILMS_BY_IDS, params, rowMapper);
    }

    @Override
    public List<Long> findUserLikedFilmIds(long userId) {
        return jdbc.queryForList(USER_LIKES_BY_ID, Long.class, userId);
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
}