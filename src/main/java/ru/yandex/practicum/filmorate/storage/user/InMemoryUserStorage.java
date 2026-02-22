package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users;
    private final Map<Long, Set<Long>> userFriends;

    @Override
    public Collection<User> getAll() {
        return users.values();
    }

    @Override
    public User save(User newUser) {
        long newId = generateId();
        newUser.setId(newId);

        users.put(newId, newUser);

        return newUser;
    }

    @Override
    public User update(User newUser) {
        users.put(newUser.getId(), newUser);

        return newUser;
    }

    @Override
    public User getUserById(long id) {
        return users.get(id);
    }

    @Override
    public void addFriendship(long userId, long friendId) {
        userFriends.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
        userFriends.computeIfAbsent(friendId, k -> new HashSet<>()).add(userId);
    }

    @Override
    public Set<Long> getUserFriendsIds(long userId) {
        return userFriends.getOrDefault(userId, Collections.emptySet());
    }

    @Override
    public void clearAll() {
        users.clear();
    }

    @Override
    public List<User> getUserFriends(long userId) {
        return userFriends.getOrDefault(userId, new HashSet<>())
                .stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void removeFriendship(long userId, long friendId) {
        userFriends.computeIfPresent(userId, (id, friends) -> {
            friends.remove(friendId);
            return friends.isEmpty() ? null : friends;
        });

        userFriends.computeIfPresent(friendId, (id, friends) -> {
            friends.remove(userId);
            return friends.isEmpty() ? null : friends;
        });
    }

    @Override
    public List<User> getUsersCommonFriends(long userId, long otherUserId) {
        Set<Long> friendsIds = getUserFriendsIds(userId);
        Set<Long> otherFriendsIds = getUserFriendsIds(otherUserId);

        return friendsIds.stream()
                .filter(otherFriendsIds::contains)
                .map(this::getUserById)
                .toList();
    }

    @Override
    public boolean isFriend(long userId, long friendId) {
        Set<Long> friendsIds = getUserFriendsIds(userId);
        return friendsIds.contains(friendId);
    }

    @Override
    public boolean isExistById(long userId) {
        return users.containsKey(userId);
    }

    private Long generateId() {
        long currentId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);

        log.debug("Generated new ID for User. New ID: {}", currentId + 1);

        return ++currentId;
    }
}
