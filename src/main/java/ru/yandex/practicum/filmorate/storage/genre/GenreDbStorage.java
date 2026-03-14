package ru.yandex.practicum.filmorate.storage.genre;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class GenreDbStorage extends BaseDbStorage<Genre> implements GenreStorage {
    private static final String FIND_ALL_GENRE = "SELECT * FROM genres";
    private static final String FIND_GENRE_BY_ID = "SELECT * FROM genres WHERE id = ?";
    private static final String FIND_GENRE_BY_IDS = "SELECT * FROM genres WHERE id IN (:ids)";
    private static final String FIND_GENRES_IDS = "SELECT id FROM genres WHERE id IN (:ids)";

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
}