package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository("userDbStorage")
public class UserDbStorage extends BaseDbStorage<User> implements UserStorage {
    private static final String FIND_ALL_USERS = "SELECT * FROM users";

    private static final String INSERT_USER = """
    INSERT INTO users (login, name, email, birthday, created_at)
    VALUES (?, ?, ?, ?, ?)
    """;

    private static final String UPDATE_USERS_QUERY = """
    UPDATE users
    SET login = ?, name = ?, email = ?, birthday = ?
    WHERE id = ?
    """;

    private static final String FIND_USER_BY_ID_QUERY = "SELECT * FROM users WHERE id = ?";

    private static final String USER_EXISTS_QUERY = "SELECT EXISTS(SELECT 1 FROM users WHERE id = ?)";

    private static final String EXIST_USER_FRIENDSHIP_QUERY = """
    SELECT EXISTS(
        SELECT 1
        FROM friendships
        WHERE user_id = ? AND friend_id = ?
    )
    """;

    private static final String FIND_USER_FRIENDS_QUERY = """
    SELECT u.*
    FROM users u
    WHERE u.id IN (
        SELECT friend_id
        FROM friendships f
        WHERE f.user_id = ?
    )
    """;

    private static final String FIND_FRIEND_IDS_QUERY = "SELECT friend_id FROM friendships WHERE user_id = ?";

    private static final String INSERT_FRIENDSHIP_QUERY = """
    INSERT INTO friendships (user_id, friend_id, created_at)
    VALUES (?, ?, ?)
    """;

    private static final String DELETE_FRIENDSHIP_QUERY = """
    DELETE FROM friendships
    WHERE (user_id = ? AND friend_id = ?)
       OR (user_id = ? AND friend_id = ?)
    """;

    private static final String FIND_COMMON_FRIENDS = """
    SELECT *
    FROM users
    WHERE id IN (
        SELECT f1.friend_id
        FROM friendships f1
        JOIN friendships f2 ON f1.friend_id = f2.friend_id
        WHERE f1.user_id = ? AND f2.user_id = ?
    )
    """;

    public UserDbStorage(JdbcTemplate jdbcTemplate, RowMapper<User> rowMapper) {
        super(jdbcTemplate, rowMapper);
    }

    @Override
    public List<User> findAll() {
        return findMany(FIND_ALL_USERS);
    }

    @Override
    public User save(User newUser) {
        long id = insert(INSERT_USER,
                newUser.getLogin(),
                newUser.getName(),
                newUser.getEmail(),
                newUser.getBirthday(),
                LocalDateTime.now()
        );

        newUser.setId(id);
        return newUser;
    }

    @Override
    public User update(User newUser) {
        update(
                UPDATE_USERS_QUERY,
                newUser.getLogin(),
                newUser.getName(),
                newUser.getEmail(),
                newUser.getBirthday(),
                newUser.getId()
        );
        return newUser;
    }

    @Override
    public Optional<User> getUserById(long id) {
        return findOne(FIND_USER_BY_ID_QUERY, id);
    }

    @Override
    public void addFriendship(long userId, long friendId) {
        insert(INSERT_FRIENDSHIP_QUERY, userId, friendId, LocalDateTime.now());
    }

    @Override
    public Set<Long> getUserFriendsIds(long userId) {
        List<Long> friendIds = jdbc.queryForList(FIND_FRIEND_IDS_QUERY, Long.class, userId);
        return new HashSet<>(friendIds);
    }

    @Override
    public void clearAll() {
        // nothing
    }

    @Override
    public List<User> getUserFriends(long userId) {
        return findMany(FIND_USER_FRIENDS_QUERY, userId);
    }

    @Override
    public void removeFriendship(long userId, long friendId) {
        update(
                DELETE_FRIENDSHIP_QUERY,
                userId,
                friendId,
                friendId,
                userId
        );
    }

    @Override
    public boolean isFriend(long userId, long friendId) {
        return isExistOne(EXIST_USER_FRIENDSHIP_QUERY, userId, friendId);
    }

    @Override
    public boolean isExistById(long userId) {
        return isExistOne(USER_EXISTS_QUERY, userId);
    }

    @Override
    public List<User> getUsersCommonFriends(long userId, long otherUserId) {
        return findMany(FIND_COMMON_FRIENDS, userId, otherUserId);
    }
}
