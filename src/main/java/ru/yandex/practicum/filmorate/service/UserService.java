package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.user.CreateUserDto;
import ru.yandex.practicum.filmorate.dto.user.UpdateUserDto;
import ru.yandex.practicum.filmorate.dto.user.UserDto;
import ru.yandex.practicum.filmorate.exception.ErrorMessages;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.UserMapper;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import org.springframework.context.annotation.Lazy; //добавлено

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    private final UserStorage userStorage;
    private final FeedService feedService; // <-- ДОБАВЛЕНО

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage,
                       @Lazy FeedService feedService) { // <-- ДОБАВЛЕНО
        this.userStorage = userStorage;
        this.feedService = feedService; // <-- ДОБАВЛЕНО
    }

    public List<UserDto> findAllUsers() {
        log.debug("Retrieving all users from storage");

        List<User> users = userStorage.findAll();

        return users
                .stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }

    public UserDto createUser(CreateUserDto requestUserDto) {
        User newUser = UserMapper.toEntity(requestUserDto);

        log.debug("Starting user creation: {}", newUser);


        // Если имя не указано, то используем логин как имя
        if (newUser.getName() == null || newUser.getName().isBlank()) {
            log.info("User name is null. Setting name to login value. User ID: {}, Login: {}",
                    newUser.getId(), newUser.getLogin());
            newUser.setName(newUser.getLogin());
        }

        User createdUser = userStorage.save(newUser);
        return UserMapper.toDto(createdUser);
    }

    public UserDto updateUser(UpdateUserDto updateUserDto) {
        log.debug("Starting update user. ID: {}, Login: {}",
                updateUserDto.getId(), updateUserDto.getLogin());

        User updatedUser = userStorage.getUserById(updateUserDto.getId())
                .map(user -> UserMapper.updateUserFields(user, updateUserDto))
                .orElseThrow(
                        () -> new NotFoundException(ErrorMessages.userNotFound(updateUserDto.getId()))
                );

        updatedUser = userStorage.update(updatedUser);
        return UserMapper.toDto(updatedUser);
    }

    public UserDto getUser(long userId) {
        log.debug("Retrieving user by ID: {}", userId);

        return userStorage.getUserById(userId)
                .map(UserMapper::toDto)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.userNotFound(userId)));
    }

    // метод для удаления пользователя
    public void deleteUser(Long userId) {
        log.debug("Deleting user with ID: {}", userId);
        validateUserExists(userId);
        userStorage.delete(userId);
        log.info("User {} deleted successfully", userId);
    }

    public void addFriend(long userId, long friendId) {
        checkUsersCanInteract(userId, friendId);

        log.debug("Attempting to add friend relationship. User ID: {}, Friend ID: {}", userId, friendId);

        if (areFriends(userId, friendId)) {
            log.warn("Friendship already exists between user {} and user {}", userId, friendId);
            return;
        }

        userStorage.addFriendship(userId, friendId);

        feedService.logEvent(userId, EventType.FRIEND, Operation.ADD, friendId);

        log.info("Friendship added between user {} and user {}", userId, friendId);
    }

    public List<UserDto> getUserFriends(long userId) {
        validateUserExists(userId);

        log.debug("Retrieving friends for user ID: {}", userId);

        return userStorage.getUserFriends(userId).stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }

    public void removeFriend(long userId, long friendId) {
        checkUsersCanInteract(userId, friendId);

        log.debug("Attempting to remove friend relationship. User ID: {}, Friend ID: {}", userId, friendId);

        if (areFriends(userId, friendId)) {
            userStorage.removeFriendship(userId, friendId);

            // Логируем событие удаления друга в ленту
            feedService.logEvent(userId, EventType.FRIEND, Operation.REMOVE, friendId);

            log.info("Friendship removed between user {} and user {}", userId, friendId);
        } else {
            log.warn("Friendship not found between user {} and user {}", userId, friendId);
        }
    }

    public List<UserDto> getCommonFriends(long userId, long otherUserId) {
        checkUsersCanInteract(userId, otherUserId);

        log.debug("Finding common friends between user {} and user {}", userId, otherUserId);
        List<User> commonFriends = userStorage.getUsersCommonFriends(userId, otherUserId);

        return commonFriends.stream()
                .map(UserMapper::toDto)
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
            throw new ValidationException(ErrorMessages.SELF_INTERACTION);
        }
        validateUserExists(userId);
        validateUserExists(otherUserId);
    }

    private void validateUserExists(long userId) {
        if (!userStorage.isExistById(userId)) {
            throw new NotFoundException(ErrorMessages.userNotFound(userId));
        }
    }
}