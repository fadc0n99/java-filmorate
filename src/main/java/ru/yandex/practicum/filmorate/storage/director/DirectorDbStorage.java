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

    private final NamedParameterJdbcTemplate namedJdbc;

    public DirectorDbStorage(JdbcTemplate jdbc,
                             RowMapper<Director> rowMapper) {
        super(jdbc, rowMapper);
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    @Override
    public List<Director> findAll() {
        String sql = "SELECT * FROM directors ORDER BY director_id";
        return findMany(sql);
    }

    @Override
    public Optional<Director> findById(Integer id) {
        String sql = "SELECT * FROM directors WHERE director_id = ?";
        return findOne(sql, id);
    }

    @Override
    public Director create(Director director) {
        String sql = "INSERT INTO directors (director_name) VALUES (?)";
        long id = insert(sql, director.getName());
        director.setId((int) id);
        return director;
    }

    @Override
    public Director update(Director director) {
        String sql = "UPDATE directors SET director_name = ? WHERE director_id = ?";
        update(sql, director.getName(), director.getId());
        return director;
    }

    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM directors WHERE director_id = ?";
        super.delete(sql, id);
    }

    @Override
    public boolean isExistById(Integer id) {
        String sql = "SELECT EXISTS(SELECT 1 FROM directors WHERE director_id = ?)";
        return isExistOne(sql, id);
    }

    @Override
    public boolean isExistByIds(List<Integer> directorsIds) {
        String sql = "SELECT EXISTS(SELECT 1 FROM directors WHERE director_id IN (:ids))";

        MapSqlParameterSource params = new MapSqlParameterSource("ids", directorsIds);

        return Boolean.TRUE.equals(namedJdbc.queryForObject(sql, params, Boolean.class));
    }
}
