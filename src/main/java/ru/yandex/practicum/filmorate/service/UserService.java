package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserStorage userStorage;

    private static final String USER_NOT_FOUND_MESSAGE = "User with ID %d not found";
    private static final String INVALID_USER_ID_MESSAGE = "Invalid user ID: must be greater than 0";
    private static final String SELF_INTERACTION_MESSAGE = "Users cannot interact with themselves";

    public Collection<User> findAllUsers() {
        log.debug("Retrieving all users from storage");

        Collection<User> users = userStorage.getAll();
        log.debug("Retrieved {} users", users.size());

        return users;
    }

    public User createUser(User newUser) {
        log.debug("Starting user creation: {}", newUser);

        // Если имя не указано, то используем логин как имя
        if (newUser.getName() == null) {
            log.info("User name is null. Setting name to login value. User ID: {}, Login: {}",
                    newUser.getId(), newUser.getLogin());
            newUser.setName(newUser.getLogin());
        }

        User createdUser = userStorage.save(newUser);
        log.info("User creation completed. ID: {}, Login: {}", createdUser.getId(), createdUser.getLogin());
        return createdUser;
    }

    public User updateUser(User newUser) {
        requireValidUser(newUser.getId());

        log.debug("Starting update user. ID: {}, Login: {}", newUser.getId(), newUser.getLogin());

        User updatedUser = userStorage.update(newUser);

        log.info("User update completed. ID: {}, Login: {}", updatedUser.getId(), updatedUser.getLogin());

        return updatedUser;
    }

    public User getUser(long userId) {
        requireValidUser(userId);

        log.debug("Retrieving user by ID: {}", userId);
        User user = userStorage.getUserById(userId);
        log.trace("Retrieved film details: {}", user);

        return user;
    }

    public void addFriend(long userId, long friendId) {
        checkUsersCanInteract(userId, friendId);

        log.debug("Attempting to add friend relationship. User ID: {}, Friend ID: {}", userId, friendId);

        if (areFriends(userId, friendId)) {
            log.info("Friendship already exists between user {} and user {}", userId, friendId);
            return;
        }

        userStorage.addFriendship(userId, friendId);
        log.info("Friendship added between user {} and user {}", userId, friendId);
    }

    public List<User> getUserFriends(long userId) {
        requireValidUser(userId);

        log.debug("Retrieving friends for user ID: {}", userId);

        List<User> friends = userStorage.getUserFriends(userId);

        log.trace("Retrieved {} friends for user ID: {}", friends.size(), userId);
        return friends;
    }

    public void removeFriend(long userId, long friendId) {
        checkUsersCanInteract(userId, friendId);

        log.debug("Attempting to remove friend relationship. User ID: {}, Friend ID: {}", userId, friendId);

        if (areFriends(userId, friendId)) {
            userStorage.removeFriendship(userId, friendId);
            log.info("Friendship removed between user {} and user {}", userId, friendId);
        } else {
            log.warn("Friendship not found between user {} and user {}", userId, friendId);
        }
    }

    public List<User> getCommonFriends(long userId, long otherUserId) {
        checkUsersCanInteract(userId, otherUserId);

        log.debug("Finding common friends between user {} and user {}", userId, otherUserId);

        List<User> commonFriends = userStorage.getUsersCommonFriends(userId, otherUserId);

        log.debug("Found {} common friends", commonFriends.size());

        return commonFriends;
    }

    private boolean areFriends(long userId, long friendId) {
        boolean isFriend = userStorage.isFriend(userId, friendId);

        log.trace("Friendship check. User ID: {}, Friend ID: {}, Result: {}", userId, friendId, isFriend);

        return isFriend;
    }

    private void checkUsersCanInteract(long userId, long otherUserId) {
        if (userId == otherUserId) {
            log.info("User {} attempted to interact with themselves", userId);
            throw new ValidationException(SELF_INTERACTION_MESSAGE);
        }
        requireValidUser(userId);
        requireValidUser(otherUserId);
    }

    public boolean isUserExists(long userId) {
        return userStorage.isExistById(userId);
    }

    public void requireValidUser(long userId) {
        if (userId <= 0) {
            throw new ValidationException(INVALID_USER_ID_MESSAGE);
        }
        if (!isUserExists(userId)) {
            throw new NotFoundException(String.format(USER_NOT_FOUND_MESSAGE, userId));
        }
    }

}