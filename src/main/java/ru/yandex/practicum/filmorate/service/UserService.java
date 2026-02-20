package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserService {

    private final Map<Long, User> users = new HashMap<>();

    public Collection<User> findAllUsers() {
        return users.values();
    }

    public User createUser(User newUser) {
        newUser.setId(generateId());

        validateLogin(newUser);

        // Если имя не указано, то используем логин как имя
        if (newUser.getName() == null) {
            log.info("User name is null. Setting name to login value. User ID: {}, Login: {}",
                    newUser.getId(), newUser.getLogin());
            newUser.setName(newUser.getLogin());
        }

        users.put(newUser.getId(), newUser);

        return newUser;
    }

    public User updateUser(User newUser) {
        if (newUser.getId() == null) {
            log.warn("User not be update: missing ID. Request data: {}", newUser);
            throw new ValidationException("ID not be null or empty");
        }

        validateLogin(newUser);

        if (users.containsKey(newUser.getId())) {
            log.debug("Starting update user. ID: {}, Login: {}", newUser.getId(), newUser.getLogin());
            User oldUser = users.get(newUser.getId());

            log.trace("Current user data: {}", oldUser);

            if (newUser.getName() != null && !newUser.getName().isBlank()) {
                oldUser.setName(newUser.getName());
            }

            oldUser.setLogin(newUser.getLogin());
            oldUser.setEmail(newUser.getEmail());
            oldUser.setBirthday(newUser.getBirthday());

            return oldUser;
        }

        throw new NotFoundException("User with ID " + newUser.getId() + " not found");
    }

    private void validateLogin(User newUser) {
        if (newUser.getLogin() != null && newUser.getLogin().contains(" ")) {
            log.warn("User create error. Invalid login: {}", newUser.getLogin());
            throw new ValidationException("User login must not contain spaces");
        }
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
