package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserStorage {

    List<User> findAll();

    User save(User newUser);

    User update(User newUser);

    Optional<User> getUserById(long id);

    void addFriendship(long userId, long friendId);

    Set<Long> getUserFriendsIds(long userId);

    List<User> getUserFriends(long userId);

    void removeFriendship(long userId, long friendId);

    boolean isFriend(long userId, long friendId);

    boolean isExistById(long userId);

    List<User> getUsersCommonFriends(long userId, long otherUserId);
}
