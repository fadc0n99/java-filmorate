package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users;

    @Override
    public List<User> findAll() {
        return users.values().stream().toList();
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
    public Optional<User> getUserById(long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public void addFriendship(long userId, long friendId) {
        User user = users.get(userId);
        User friend = users.get(friendId);

        if (user == null || friend == null) {
            throw new NotFoundException("User not found: userId=" + userId + ", friendId=" + friendId);
        }

        Set<Long> userFriendsIds = user.getFriendsIds();
        Set<Long> otherFriendsIds = friend.getFriendsIds();

        if (userFriendsIds != null) {
            userFriendsIds.add(friendId);
        } else {
            user.setFriendsIds(new HashSet<>(Set.of(friendId)));
        }
        if (otherFriendsIds != null) {
            otherFriendsIds.add(userId);
        } else {
            friend.setFriendsIds(new HashSet<>(Set.of(userId)));
        }
    }

    @Override
    public Set<Long> getUserFriendsIds(long userId) {
        User user = users.get(userId);
        if (user == null) {
            log.warn("User with ID {} not found", userId);
            return Collections.emptySet();
        }
        return user.getFriendsIds() != null ? user.getFriendsIds() : Collections.emptySet();
    }

    @Override
    public void clearAll() {
        users.clear();
    }

    @Override
    public List<User> getUserFriends(long userId) {
        return Optional.ofNullable(users.get(userId))
                .map(User::getFriendsIds)
                .map(friendIds -> friendIds.stream()
                        .map(this::getUserById)
                        .flatMap(Optional::stream)
                        .collect(Collectors.toList()))
                .orElseGet(Collections::emptyList);
    }

    @Override
    public void removeFriendship(long userId, long friendId) {
        Set<Long> userFriendsIds = users.get(userId).getFriendsIds();
        Set<Long> otherFriendsIds = users.get(friendId).getFriendsIds();

        if (userFriendsIds != null) {
            userFriendsIds.remove(friendId);
        }
        if (otherFriendsIds != null) {
            otherFriendsIds.remove(userId);
        }
    }

    @Override
    public List<User> getUsersCommonFriends(long userId, long otherUserId) {
        Set<Long> friendsIds = getUserFriendsIds(userId);
        Set<Long> otherFriendsIds = getUserFriendsIds(otherUserId);

        if (friendsIds == null || otherFriendsIds == null) {
            return Collections.emptyList();
        }

        return friendsIds.stream()
                .filter(otherFriendsIds::contains)
                .map(this::getUserById)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public boolean isFriend(long userId, long friendId) {
        Set<Long> friendsIds = getUserFriendsIds(userId);
        return friendsIds != null && friendsIds.contains(friendId);
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
