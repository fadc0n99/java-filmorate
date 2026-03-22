package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public abstract class BaseDbStorage<T> {
    protected final JdbcTemplate jdbc;
    protected final RowMapper<T> rowMapper;

    protected List<T> findMany(String query, Object... params) {
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

    protected long insert(String query, Object... params) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if (key != null) {
            return key.longValue();
        } else {
            throw new RuntimeException("Ошибка при записи данных: ID не получен");
        }
    }

    protected void update(String query, Object... params) {
        int updatedRows = jdbc.update(query, params);
        if (updatedRows == 0) {
            throw new NotFoundException("Данные для обновления не найдены");
        }
    }

    protected boolean delete(String query, Object... params) {
        int rowsAffected = jdbc.update(query, params);
        return rowsAffected > 0;
    }

    protected boolean isExistOne(String query, Object... params) {
        Boolean exists = jdbc.queryForObject(query, Boolean.class, params);
        return Boolean.TRUE.equals(exists);
    }

    protected void batchUpdate(String query, List<Object[]> batchArgs) {
        if (!batchArgs.isEmpty()) {
            jdbc.batchUpdate(query, batchArgs);
        }
    }
}
