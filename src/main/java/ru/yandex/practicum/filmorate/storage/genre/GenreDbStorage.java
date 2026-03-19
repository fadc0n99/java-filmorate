package ru.yandex.practicum.filmorate.storage.genre;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.util.*;

@Repository
public class GenreDbStorage extends BaseDbStorage<Genre> implements GenreStorage {
    private static final String FIND_ALL_GENRE = "SELECT * FROM genres";
    private static final String FIND_GENRE_BY_ID = "SELECT * FROM genres WHERE id = ?";
    private static final String FIND_GENRE_BY_IDS = "SELECT * FROM genres WHERE id IN (:ids)";
    private static final String FIND_GENRES_IDS = "SELECT id FROM genres WHERE id IN (:ids)";
    private static final String FIND_GENRES_BY_FILM_IDS = """
            SELECT fg.film_id, fg.genre_id as id, g.name
            FROM film_genres fg
            LEFT JOIN genres g ON fg.genre_id = g.id
            WHERE fg.film_id IN (:filmIds)
            """;

    private final NamedParameterJdbcTemplate namedJdbc;

    public GenreDbStorage(JdbcTemplate jdbc, RowMapper<Genre> rowMapper) {
        super(jdbc, rowMapper);
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    @Override
    public List<Genre> findAll() {
        return findMany(FIND_ALL_GENRE);
    }

    @Override
    public Optional<Genre> findById(long id) {
        return findOne(FIND_GENRE_BY_ID, id);
    }

    @Override
    public List<Genre> findGenresByIds(List<Long> genreIds) {
        Set<Long> uniqueIds = new HashSet<>(genreIds);

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("ids", uniqueIds);

        return namedJdbc.query(FIND_GENRE_BY_IDS, parameters, rowMapper);
    }

    @Override
    public List<Long> findGenreIdsByIds(List<Long> genreIds) {
        Set<Long> uniqueGenreIds = new HashSet<>(genreIds);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", uniqueGenreIds);

        return namedJdbc.queryForList(FIND_GENRES_IDS, params, Long.class);
    }

    @Override
    public Map<Long, List<Genre>> findGenresByFilmIds(List<Long> filmIds) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("filmIds", filmIds);

        return namedJdbc.query(FIND_GENRES_BY_FILM_IDS, parameters, rs -> {
            Map<Long, List<Genre>> result = new HashMap<>();

            while (rs.next()) {
                long filmId = rs.getLong("film_id");
                Genre genre = rowMapper.mapRow(rs, rs.getRow());

                result.computeIfAbsent(filmId, v -> new ArrayList<>()).add(genre);
            }

            return result;
        });
    }
}