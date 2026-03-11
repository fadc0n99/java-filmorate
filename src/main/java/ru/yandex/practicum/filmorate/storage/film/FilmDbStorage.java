package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dto.GenreId;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository("filmDbStorage")
public class FilmDbStorage extends BaseDbStorage<Film> implements FilmStorage {

    /**
     * Film queries
     */
    private static final String FIND_ALL_FILMS = "SELECT * FROM films";

    private static final String FIND_FILM_BY_ID = "SELECT * FROM films WHERE id = ?";

    private static final String INSERT_FILM_QUERY = """
        INSERT INTO films (name, description, release_date, duration, mpa_rating_id, created_at)
        VALUES (?, ?, ?, ?, ?, ?)
        """;

    private static final String DELETE_FILM_GENRES_QUERY =
            "DELETE FROM film_genres WHERE film_id = ? AND genre_id = ?";

    private static final String INSERT_FILM_GENRES_QUERY = """
        MERGE INTO film_genres (film_id, genre_id)
        KEY (film_id, genre_id)
        VALUES (?, ?)
        """;

    private static final String UPDATE_FILM_QUERY = """
        UPDATE films
        SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ?
        WHERE id = ?
        """;

    private static final String FIND_GENRES_BY_ID = """
        SELECT COUNT(*)
        FROM genres
        WHERE ID IN (:genreIds)
        """;

    private static final String EXIST_MPA_ID = "SELECT EXISTS(SELECT 1 FROM mpa_ratings WHERE id = ?)";

    private static final String FIND_FILM_GENRES_BY_IDS = """
        SELECT film_id, genre_id
        FROM film_genres
        WHERE film_id IN (:filmIds)
        ORDER BY id
        """;

    private static final String EXISTS_FILM_BY_ID = "SELECT EXISTS(SELECT 1 FROM films WHERE id = ?)";

    private static final String FIND_POPULAR_FILMS = """
        SELECT f.*
        FROM films f
        JOIN (
            SELECT film_id, COUNT(*) AS likes_count
            FROM film_likes
            GROUP BY film_id
        ) AS film_stats ON f.id = film_stats.film_id
        ORDER BY film_stats.likes_count DESC
        LIMIT ?
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

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    public FilmDbStorage(JdbcTemplate jdbc, RowMapper<Film> rowMapper) {
        super(jdbc, rowMapper);
    }

    @Override
    public List<Film> findAll() {
        List<Film> films = findMany(FIND_ALL_FILMS);

        fillMoviesGenres(films);

        return films;
    }

    private void fillMoviesGenres(List<Film> films) {
        List<Long> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toList());

        Map<Long, List<GenreId>> filmGenresMap = groupGenresByMovies(filmIds);
        films.forEach(film -> {
            List<GenreId> genres = filmGenresMap.getOrDefault(film.getId(), Collections.emptyList());
            film.setGenresId(genres);
        });
    }

    private Map<Long, List<GenreId>> groupGenresByMovies(List<Long> filmIds) {
        if (filmIds.isEmpty()) {
            return Collections.emptyMap();
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource("filmIds", filmIds);
        List<Map<String, Object>> genreMappings = namedJdbcTemplate.queryForList
                (FIND_FILM_GENRES_BY_IDS, parameters);

        return genreMappings.stream()
                .collect(Collectors.groupingBy(
                        row -> ((Number) row.get("film_id")).longValue(),
                        Collectors.mapping(
                                row -> GenreId.of(((Number) row.get("genre_id")).longValue()),
                                Collectors.toList()
                        )
                ));
    }

    @Override
    public Film save(Film newFilm) {
        long id = insert(
                INSERT_FILM_QUERY,
                newFilm.getName(),
                newFilm.getDescription(),
                newFilm.getReleaseDate(),
                newFilm.getDuration(),
                newFilm.getMpaId().getId(),
                LocalDateTime.now()
        );
        newFilm.setId(id);

        List<GenreId> uniqueGenres = newFilm.getGenresId().stream()
                .distinct()
                .toList();

        for (GenreId genreId : uniqueGenres) {
            update(INSERT_FILM_GENRES_QUERY, id, genreId.getId());
        }

        return newFilm;
    }

    @Override
    public Film update(Film newFilm) {
        update(
                UPDATE_FILM_QUERY,
                newFilm.getName(),
                newFilm.getDescription(),
                newFilm.getReleaseDate(),
                newFilm.getDuration(),
                newFilm.getMpaId().getId(),
                newFilm.getId()
        );

        updateGenres(newFilm);
        fillMoviesGenres(List.of(newFilm));

        return newFilm;
    }

    private void updateGenres(Film newFilm) {
        Set<Long> currentGenreIds = new HashSet<>(getCurrentGenresIds(newFilm.getId()));
        Set<Long> newGenreIds = newFilm.getGenresId().stream()
                .map(GenreId::getId)
                .collect(Collectors.toSet());

        if (currentGenreIds.equals(newGenreIds)) {
            return;
        }

        List<Long> genresToDelete = currentGenreIds.stream()
                .filter(id -> !newGenreIds.contains(id))
                .toList();
        List<Long> genresToAdd = newGenreIds.stream()
                .filter(id -> !currentGenreIds.contains(id))
                .toList();

        genresToDelete.forEach(id -> update(DELETE_FILM_GENRES_QUERY, newFilm.getId(), id));
        genresToAdd.forEach(id -> insert(INSERT_FILM_GENRES_QUERY, newFilm.getId(), id));
    }

    private List<Long> getCurrentGenresIds(Long id) {
        String sql = "SELECT DISTINCT genre_id FROM film_genres WHERE film_id = ?";
        return jdbc.query(sql, (rs, rowNum) ->
                rs.getLong("genre_id"), id);
    }

    @Override
    public Optional<Film> findFilmById(long id) {
        Optional<Film> film = findOne(FIND_FILM_BY_ID, id);

        film.ifPresent(value -> fillMoviesGenres(List.of(value)));

        return film;
    }

    @Override
    public void clearAll() {
        // do nothing
    }

    @Override
    public boolean isExistById(long filmId) {
        return isExistOne(EXISTS_FILM_BY_ID, filmId);
    }

    @Override
    public void addLike(long filmId, long userId) {
        insert(INSERT_LIKE_FILM, filmId, userId, LocalDateTime.now());
    }

    @Override
    public boolean isLikeExists(long filmId, long userId) {
        return isExistOne(EXIST_USER_LIKE, filmId, userId);
    }

    @Override
    public void removeLike(long filmId, long userId) {
        update(DELETE_LIKE, filmId, userId);
    }

    @Override
    public Set<Long> getLikes(Long id) {
        return Set.of();
    }

    @Override
    public boolean isMpaExist(Long mpaId) {
        return isExistOne(EXIST_MPA_ID, mpaId);
    }

    @Override
    public boolean isGenresExist(List<GenreId> genresId) {
        Set<Long> genreIds = genresId.stream()
                .map(GenreId::getId)
                .collect(Collectors.toSet());

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("genreIds", genreIds);

        Integer genresCount = namedJdbcTemplate.queryForObject(FIND_GENRES_BY_ID, params, Integer.class);

        return genresCount != null && genresCount == genresId.size();
    }

    @Override
    public List<Film> getPopularFilms(long limit) {
        List<Film> popularFilms = findMany(FIND_POPULAR_FILMS, limit);

        fillMoviesGenres(popularFilms);

        return popularFilms;
    }
}
