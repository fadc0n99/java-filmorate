package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.CreateUserDto;
import ru.yandex.practicum.filmorate.dto.UpdateUserDto;
import ru.yandex.practicum.filmorate.dto.UserDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.UserMapper;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    private final UserStorage userStorage;

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    private static final String USER_NOT_FOUND_MESSAGE = "User with ID %d not found";
    private static final String INVALID_USER_ID_MESSAGE = "Invalid user ID: must be greater than 0";
    private static final String SELF_INTERACTION_MESSAGE = "Users cannot interact with themselves";

    public List<UserDto> findAllUsers() {
        log.debug("Retrieving all users from storage");

        List<User> users = userStorage.findAll();
        log.debug("Retrieved {} users", users.size());

        return users
                .stream()
                .map(UserMapper::mapToUserDto)
                .collect(Collectors.toList());
    }

    public UserDto createUser(CreateUserDto requestUserDto) {
        User newUser = UserMapper.mapToUser(requestUserDto);

        log.debug("Starting user creation: {}", newUser);


        // Если имя не указано, то используем логин как имя
        if (newUser.getName() == null) {
            log.info("User name is null. Setting name to login value. User ID: {}, Login: {}",
                    newUser.getId(), newUser.getLogin());
            newUser.setName(newUser.getLogin());
        }

        User createdUser = userStorage.save(newUser);
        return UserMapper.mapToUserDto(createdUser);
    }

    public UserDto updateUser(UpdateUserDto updateUserDto) {
        log.debug("Starting update user. ID: {}, Login: {}",
                updateUserDto.getId(), updateUserDto.getLogin());

        User updatedUser = userStorage.getUserById(updateUserDto.getId())
                .map(user -> UserMapper.updateUserFields(user, updateUserDto))
                .orElseThrow(
                        () -> new NotFoundException(
                                String.format(USER_NOT_FOUND_MESSAGE, updateUserDto.getId())
                        )
                );

        updatedUser = userStorage.update(updatedUser);
        return UserMapper.mapToUserDto(updatedUser);
    }

    public UserDto getUser(long userId) {
        log.debug("Retrieving user by ID: {}", userId);

        return userStorage.getUserById(userId)
                .map(UserMapper::mapToUserDto)
                .orElseThrow(() -> new NotFoundException(String.format(USER_NOT_FOUND_MESSAGE, userId)));
    }

    public void addFriend(long userId, long friendId) {
        checkUsersCanInteract(userId, friendId);

        log.debug("Attempting to add friend relationship. User ID: {}, Friend ID: {}", userId, friendId);

        if (areFriends(userId, friendId)) {
            log.warn("Friendship already exists between user {} and user {}", userId, friendId);
            return;
        }

        userStorage.addFriendship(userId, friendId);
        log.info("Friendship added between user {} and user {}", userId, friendId);
    }

    public List<UserDto> getUserFriends(long userId) {
        requireValidUser(userId);

        log.debug("Retrieving friends for user ID: {}", userId);

        return userStorage.getUserFriends(userId).stream()
                .map(UserMapper::mapToUserDto)
                .collect(Collectors.toList());
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

    public List<UserDto> getCommonFriends(long userId, long otherUserId) {
        checkUsersCanInteract(userId, otherUserId);

        log.debug("Finding common friends between user {} and user {}", userId, otherUserId);

        List<User> commonFriends = userStorage.getUsersCommonFriends(userId, otherUserId);

        log.debug("Found {} common friends", commonFriends.size());

        return commonFriends.stream()
                .map(UserMapper::mapToUserDto)
                .collect(Collectors.toList());
    }

    private boolean areFriends(long userId, long friendId) {
        boolean isFriend = userStorage.isFriend(userId, friendId);

        log.trace("Friendship check. User ID: {}, Friend ID: {}, Result: {}", userId, friendId, isFriend);

        return isFriend;
    }

    private void checkUsersCanInteract(long userId, long otherUserId) {
        if (userId == otherUserId) {
            log.error("User {} attempted to interact with themselves", userId);
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