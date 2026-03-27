package ru.yandex.practicum.filmorate.storage.director;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.util.List;
import java.util.Optional;

@Repository("directorDbStorage")
public class DirectorDbStorage extends BaseDbStorage<Director> implements DirectorStorage {

    private static final String SELECT_ALL_DIRECTORS = """
            SELECT * FROM directors ORDER BY director_id
            """;

    private static final String SELECT_DIRECTOR_BY_ID = """
            SELECT * FROM directors WHERE director_id = ?
            """;

    private static final String INSERT_DIRECTOR = """
            INSERT INTO directors (director_name) VALUES (?)
            """;

    private static final String UPDATE_DIRECTOR = """
            UPDATE directors SET director_name = ? WHERE director_id = ?
            """;

    private static final String DELETE_DIRECTOR = """
            DELETE FROM directors WHERE director_id = ?
            """;

    private static final String EXISTS_DIRECTOR_BY_ID = """
            SELECT EXISTS(SELECT 1 FROM directors WHERE director_id = ?)
            """;

    private static final String EXISTS_DIRECTORS_BY_IDS = """
            SELECT EXISTS(SELECT 1 FROM directors WHERE director_id IN (:ids))
            """;

    public DirectorDbStorage(JdbcTemplate jdbc,
                             RowMapper<Director> rowMapper,
                             NamedParameterJdbcTemplate namedJdbc) {
        super(jdbc, namedJdbc, rowMapper);
    }

    @Override
    public List<Director> findAll() {
        return findMany(SELECT_ALL_DIRECTORS);
    }

    @Override
    public Optional<Director> findById(Integer id) {
        return findOne(SELECT_DIRECTOR_BY_ID, id);
    }

    @Override
    public Director create(Director director) {
        long id = insert(INSERT_DIRECTOR, director.getName());
        director.setId((int) id);
        return director;
    }

    @Override
    public Director update(Director director) {
        update(UPDATE_DIRECTOR, director.getName(), director.getId());
        return director;
    }

    @Override
    public void delete(Integer id) {
        delete(DELETE_DIRECTOR, id);
    }

    @Override
    public boolean isExistById(Integer id) {
        return exists(EXISTS_DIRECTOR_BY_ID, id);
    }

    @Override
    public boolean isExistByIds(List<Integer> directorsIds) {
        MapSqlParameterSource params = new MapSqlParameterSource("ids", directorsIds);
        return exists(EXISTS_DIRECTORS_BY_IDS, params);
    }
}