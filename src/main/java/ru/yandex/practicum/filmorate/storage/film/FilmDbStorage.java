package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.time.LocalDateTime;
import java.util.*;

@Repository("filmDbStorage")
public class FilmDbStorage extends BaseDbStorage<Film> implements FilmStorage {

    private static final String FIND_ALL_FILMS = "SELECT * FROM films";
    private static final String FIND_FILM_BY_ID = "SELECT * FROM films WHERE id = ?";
    private static final String EXISTS_FILM_BY_ID = "SELECT EXISTS(SELECT 1 FROM films WHERE id = ?)";
    private static final String INSERT_FILM_QUERY = """
            INSERT INTO films (name, description, release_date, duration, mpa_rating_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_FILM_QUERY = """
            UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ? WHERE id = ?
            """;
    private static final String FIND_FILMS_BY_DIRECTOR_SORT_YEAR = """
            SELECT f.* FROM films f
            JOIN film_directors fd ON f.id = fd.film_id
            WHERE fd.director_id = ?
            ORDER BY f.release_date
            """;
    private static final String FIND_FILMS_BY_DIRECTOR_SORT_LIKES = """
            SELECT f.*
            FROM films f
            JOIN film_directors fd ON f.id = fd.film_id
            LEFT JOIN film_likes fl ON f.id = fl.film_id
            WHERE fd.director_id = ?
            GROUP BY f.id
            ORDER BY COUNT(fl.user_id) DESC
            """;
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

    private static final String INSERT_FILM_DIRECTOR = "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)";
    private static final String DELETE_FILM_DIRECTORS = "DELETE FROM film_directors WHERE film_id = ?";
    private static final String INSERT_FILM_GENRES_QUERY = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
    private static final String DELETE_FILM_GENRES_QUERY = "DELETE FROM film_genres WHERE film_id = ?";
    private static final String INSERT_LIKE_FILM = "INSERT INTO film_likes (film_id, user_id, liked_at) VALUES (?, ?, ?)";
    private static final String DELETE_LIKE = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
    private static final String EXIST_USER_LIKE = "SELECT EXISTS(SELECT 1 FROM film_likes WHERE film_id = ? AND user_id = ?)";

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
    public List<Film> findAllByDirector(long directorId, String sortBy) {
        String sql = sortBy.equals("year") ? FIND_FILMS_BY_DIRECTOR_SORT_YEAR : FIND_FILMS_BY_DIRECTOR_SORT_LIKES;
        List<Film> films = findMany(sql, directorId);
        enrichFilmsData(films);
        return films;
    }

    @Override
    public Film save(Film newFilm) {
        long id = insert(INSERT_FILM_QUERY, newFilm.getName(), newFilm.getDescription(), newFilm.getReleaseDate(),
                newFilm.getDuration(), newFilm.getMpaId(), LocalDateTime.now());
        newFilm.setId(id);
        saveGenresAndDirectors(newFilm);
        return newFilm;
    }

    @Override
    public Film update(Film newFilm) {
        int rowsUpdated = jdbc.update(UPDATE_FILM_QUERY,
                newFilm.getName(),
                newFilm.getDescription(),
                newFilm.getReleaseDate(),
                newFilm.getDuration(),
                newFilm.getMpaId(),
                newFilm.getId());

        if (rowsUpdated == 0) {
            throw new NotFoundException("Фильм с id " + newFilm.getId() + " не найден");
        }

        updateGenres(newFilm);
        updateDirectors(newFilm);
        enrichFilmData(newFilm);
        return newFilm;
    }

    private void saveGenresAndDirectors(Film film) {
        if (film.getGenresIds() != null) {
            film.getGenresIds().forEach(gId -> jdbc.update(INSERT_FILM_GENRES_QUERY, film.getId(), gId));
        }
        if (film.getDirectors() != null) {
            film.getDirectors().forEach(d -> jdbc.update(INSERT_FILM_DIRECTOR, film.getId(), d.getId()));
        }
    }

    private void updateDirectors(Film film) {
        jdbc.update(DELETE_FILM_DIRECTORS, film.getId());
        if (film.getDirectors() != null) {
            film.getDirectors().forEach(d -> jdbc.update(INSERT_FILM_DIRECTOR, film.getId(), d.getId()));
        }
    }

    private void updateGenres(Film newFilm) {
        jdbc.update(DELETE_FILM_GENRES_QUERY, newFilm.getId());
        if (newFilm.getGenresIds() != null && !newFilm.getGenresIds().isEmpty()) {
            // Используем Set, чтобы избежать дубликатов жанров
            new HashSet<>(newFilm.getGenresIds()).forEach(gId ->
                    jdbc.update(INSERT_FILM_GENRES_QUERY, newFilm.getId(), gId)
            );
        }
    }

    private void enrichFilmData(Film film) {
        film.setGenresIds(getFilmGenreIds(film.getId()));
        film.setDirectors(getFilmDirectors(film.getId()));
    }

    private void enrichFilmsData(List<Film> films) {
        if (films.isEmpty()) return;
        List<Long> ids = films.stream().map(Film::getId).toList();

        Map<Long, List<Long>> genres = getGenreIdsForFilms(ids);
        Map<Long, List<Director>> directors = getDirectorsForFilms(ids);

        films.forEach(f -> {
            f.setGenresIds(genres.getOrDefault(f.getId(), Collections.emptyList()));
            f.setDirectors(directors.getOrDefault(f.getId(), new ArrayList<>()));
        });
    }

    private List<Director> getFilmDirectors(long filmId) {
        String sql = "SELECT d.* FROM directors d JOIN film_directors fd ON d.director_id = fd.director_id WHERE fd.film_id = ?";
        return jdbc.query(sql, (rs, rowNum) -> Director.builder()
                .id(rs.getInt("director_id"))
                .name(rs.getString("director_name")).build(), filmId);
    }

    private Map<Long, List<Director>> getDirectorsForFilms(List<Long> filmIds) {
        String sql = "SELECT fd.film_id, d.* FROM directors d JOIN film_directors fd ON d.director_id = fd.director_id WHERE fd.film_id IN (:filmIds)";
        MapSqlParameterSource params = new MapSqlParameterSource("filmIds", filmIds);
        return namedJdbc.query(sql, params, rs -> {
            Map<Long, List<Director>> result = new HashMap<>();
            while (rs.next()) {
                long fId = rs.getLong("film_id");
                Director d = Director.builder().id(rs.getInt("director_id")).name(rs.getString("director_name")).build();
                result.computeIfAbsent(fId, k -> new ArrayList<>()).add(d);
            }
            return result;
        });
    }

    private List<Long> getFilmGenreIds(long filmId) {
        return jdbc.query("SELECT genre_id FROM film_genres WHERE film_id = ?", (rs, rowNum) -> rs.getLong("genre_id"), filmId);
    }

    private Map<Long, List<Long>> getGenreIdsForFilms(List<Long> filmsIds) {
        MapSqlParameterSource params = new MapSqlParameterSource("filmIds", filmsIds);
        return namedJdbc.query("SELECT film_id, genre_id FROM film_genres WHERE film_id IN (:filmIds)", params, rs -> {
            Map<Long, List<Long>> result = new HashMap<>();
            while (rs.next()) {
                result.computeIfAbsent(rs.getLong("film_id"), v -> new ArrayList<>()).add(rs.getLong("genre_id"));
            }
            return result;
        });
    }

    @Override
    public boolean isExistById(long id) {
        return isExistOne(EXISTS_FILM_BY_ID, id);
    }

    @Override
    public void addLike(long fId, long uId) {
        jdbc.update(INSERT_LIKE_FILM, fId, uId, LocalDateTime.now());
    }

    @Override
    public boolean isLikeExists(long fId, long uId) {
        return isExistOne(EXIST_USER_LIKE, fId, uId);
    }

    @Override
    public void removeLike(long fId, long uId) {
        jdbc.update(DELETE_LIKE, fId, uId);
    }

    @Override
    public List<Film> getPopularFilms(long limit) {
        List<Film> popularFilms = findMany(FIND_POPULAR_FILMS, limit);
        enrichFilmsData(popularFilms);
        return popularFilms;
    }
}