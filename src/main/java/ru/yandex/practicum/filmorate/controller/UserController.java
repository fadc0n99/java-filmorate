package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Collection<User>> handleFindAll() {
        log.debug("Request received: GET /users - retrieving all users");

        Collection<User> users = userService.findAllUsers();

        log.info("Retrieved {} users successfully", users.size());

        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<User> handleCreateUser(@Valid @RequestBody User newUser) {
        log.debug("Request received: POST /users - creating new user: {}", newUser);

        User createdUser = userService.createUser(newUser);

        log.info("User created successfully. ID: {}, Login: {}", createdUser.getId(), createdUser.getLogin());
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<User> handleUpdateUser(@Valid @RequestBody User newUser) {
        log.debug("Request received: PUT /users - updating user. ID: {}, Login: {}",
                newUser.getId(), newUser.getLogin());

        User updatedUser = userService.updateUser(newUser);

        log.info("User updated successfully. ID: {}, Login: {}", updatedUser.getId(), updatedUser.getLogin());
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> handleGetUserById(@PathVariable long id) {
        log.debug("Request received: GET /users/{} - retrieving user by ID", id);

        User user = userService.getUser(id);

        log.info("User retrieved successfully. ID: {}, Login: {}", user.getId(), user.getLogin());
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping("/{id}/friends")
    public ResponseEntity<List<User>> handleGetUserFriends(@PathVariable long id) {
        log.debug("Request received: GET /users/{}/friends - retrieving friends for user", id);

        List<User> friends = userService.getUserFriends(id);

        log.info("Retrieved {} friends for user ID: {}", friends.size(), id);
        return ResponseEntity.ok(friends);
    }

    @PutMapping("/{id}/friends/{friendId}")
    public ResponseEntity<Void> handleAddFriend(@PathVariable long id, @PathVariable long friendId) {
        log.debug("Request received: PUT /users/{}/friends/{} - adding friend", id, friendId);

        userService.addFriend(id, friendId);

        log.info("Friend added successfully. User ID: {}, Friend ID: {}", id, friendId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public ResponseEntity<Void> handleRemoveFriend(@PathVariable long id, @PathVariable long friendId) {
        log.debug("Request received: DELETE /users/{}/friends/{} - removing friend", id, friendId);

        userService.removeFriend(id, friendId);

        log.info("Friend removed successfully. User ID: {}, Friend ID: {}", id, friendId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public ResponseEntity<List<User>> handleCommonFriends(@PathVariable long id, @PathVariable long otherId) {
        log.debug("Request received: GET /users/{}/friends/common/{} - finding common friends", id, otherId);

        List<User> commonFriends = userService.getCommonFriends(id, otherId);

        log.info("Found {} common friends between user {} and user {}", commonFriends.size(), id, otherId);
        return ResponseEntity.ok(commonFriends);
    }
}