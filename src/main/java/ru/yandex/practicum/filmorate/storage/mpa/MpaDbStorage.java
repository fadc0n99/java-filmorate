package ru.yandex.practicum.filmorate.storage.mpa;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class MpaDbStorage extends BaseDbStorage<Mpa> implements MpaStorage {
    private static final String FIND_ALL_MPA = "SELECT * FROM mpa_ratings";
    private static final String FIND_MPA_BY_ID = "SELECT * FROM mpa_ratings WHERE id = ?";
    private static final String FIND_MPA_BY_IDS = "SELECT * FROM mpa_ratings WHERE id IN (:ids)";

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
}