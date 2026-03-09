package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface UserStorage {

    Collection<User> getAll();

    User save(User newUser);

    User update(User newUser);

    User getUserById(long id);

    void addFriendship(long userId, long friendId);

    Set<Long> getUserFriendsIds(long userId);

    void clearAll();

    List<User> getUserFriends(long userId);

    void removeFriendship(long userId, long friendId);

    boolean isFriend(long userId, long friendId);

    boolean isExistById(long userId);

    List<User> getUsersCommonFriends(long userId, long otherUserId);
}
