package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public abstract class BaseDbStorage<T> {
    protected final JdbcTemplate jdbc;
    protected final NamedParameterJdbcTemplate namedJdbc;
    protected final RowMapper<T> rowMapper;

    protected List<T> findMany(String query, Object... params) {
        return jdbc.query(query, rowMapper, params);
    }

    protected List<T> findMany(String query, MapSqlParameterSource params) {
        return namedJdbc.query(query, params, rowMapper);
    }

    protected <R> List<R> findMany(String query, RowMapper<R> rowMapper, Object... params) {
        return jdbc.query(query, rowMapper, params);
    }

    protected Optional<T> findOne(String query, Object... params) {
        try {
            T resultRow = jdbc.queryForObject(query, rowMapper, params);
            return Optional.ofNullable(resultRow);
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    protected <R> Optional<R> findOne(String query, Class<R> requiredType, Object... params) {
        try {
            R result = jdbc.queryForObject(query, requiredType, params);
            return Optional.ofNullable(result);
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    protected <R> List<R> findList(String query, Class<R> elementType, Object... params) {
        return jdbc.queryForList(query, elementType, params);
    }

    protected <R> List<R> findList(String query, Class<R> elementType, MapSqlParameterSource params) {
        return namedJdbc.queryForList(query, params, elementType);
    }

    protected long insert(String query, Object... params) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps;
        }, keyHolder);

        return Optional.ofNullable(keyHolder.getKey())
                .map(Number::longValue)
                .orElseThrow(() ->
                        new IllegalStateException("Failed to retrieve generated key during insert operation"));
    }

    protected void update(String query, Object... params) {
        int updatedRows = jdbc.update(query, params);
        if (updatedRows == 0) {
            throw new NotFoundException("No record found to update");
        }
    }

    protected void updateWithoutCheck(String query, Object... params) {
        jdbc.update(query, params);
    }

    protected boolean delete(String query, Object... params) {
        int rowsAffected = jdbc.update(query, params);
        return rowsAffected > 0;
    }

    protected boolean exists(String query, Object... params) {
        Boolean exists = jdbc.queryForObject(query, Boolean.class, params);
        return Boolean.TRUE.equals(exists);
    }

    protected boolean exists(String query, MapSqlParameterSource params) {
        Boolean exists = namedJdbc.queryForObject(query, params, Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    protected void batchUpdate(String query, List<Object[]> batchArgs) {
        if (!batchArgs.isEmpty()) {
            jdbc.batchUpdate(query, batchArgs);
        }
    }
}
