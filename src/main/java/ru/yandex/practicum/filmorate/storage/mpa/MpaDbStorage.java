package ru.yandex.practicum.filmorate.storage.mpa;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.util.*;

@Repository
public class MpaDbStorage extends BaseDbStorage<Mpa> implements MpaStorage {
    private static final String FIND_ALL_MPA = "SELECT * FROM mpa_ratings";
    private static final String FIND_MPA_BY_ID = "SELECT * FROM mpa_ratings WHERE mpa_id = ?";
    private static final String FIND_MPA_BY_IDS = "SELECT * FROM mpa_ratings WHERE mpa_id IN (:ids)";
    private static final String EXIST_MPA_ID = "SELECT EXISTS(SELECT 1 FROM mpa_ratings WHERE mpa_id = ?)";
    private static final String FIND_MPA_BY_FILM_IDS = """
            SELECT f.id AS film_id, mr.*
            FROM films f
            LEFT JOIN mpa_ratings mr ON f.mpa_rating_id = mr.mpa_id
            WHERE f.id IN (:filmIds)
            """;

    private final NamedParameterJdbcTemplate namedJdbc;

    public MpaDbStorage(JdbcTemplate jdbc, RowMapper<Mpa> rowMapper) {
        super(jdbc, rowMapper);
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    @Override
    public List<Mpa> findAll() {
        return findMany(FIND_ALL_MPA);
    }

    @Override
    public Optional<Mpa> findById(long id) {
        return findOne(FIND_MPA_BY_ID, id);
    }

    @Override
    public Set<Mpa> findByIdIn(Set<Long> mpaIds) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("ids", mpaIds);

        return new HashSet<>(namedJdbc.query(FIND_MPA_BY_IDS, parameters, rowMapper));
    }

    @Override
    public boolean isExistById(Long mpaId) {
        return isExistOne(EXIST_MPA_ID, mpaId);
    }

    @Override
    public Map<Long, Mpa> findMpasByFilmIds(List<Long> filmIds) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("filmIds", filmIds);

        return namedJdbc.query(FIND_MPA_BY_FILM_IDS, parameters, rs -> {
            Map<Long, Mpa> result = new HashMap<>();

            while (rs.next()) {
                long filmId = rs.getLong("film_id");
                Mpa mpa = rowMapper.mapRow(rs, rs.getRow());

                result.put(filmId, mpa);
            }

            return result;
        });
    }
}