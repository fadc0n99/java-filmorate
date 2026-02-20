package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.Collection;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Collection<User>> findAll() {
        Collection<User> users = userService.findAllUsers();

        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody User newUser) {
        log.debug("Creating user: {}", newUser);

        User createdUser = userService.createUser(newUser);

        log.info("User created. ID: {}, Login: {}", createdUser.getId(), createdUser.getLogin());

        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<User> update(@Valid @RequestBody User newUser) {
        log.debug("Updating user. ID: {}", newUser.getId());

        User updatedUser = userService.updateUser(newUser);

        log.info("User updated. ID: {}, Login: {}", updatedUser.getId(), updatedUser.getLogin());
        log.trace("Updated user data: {}", updatedUser);

        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }
}
