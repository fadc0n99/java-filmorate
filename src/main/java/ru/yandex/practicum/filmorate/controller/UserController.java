package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.film.FilmDto;
import ru.yandex.practicum.filmorate.dto.user.CreateUserDto;
import ru.yandex.practicum.filmorate.dto.user.UpdateUserDto;
import ru.yandex.practicum.filmorate.dto.user.UserDto;
import ru.yandex.practicum.filmorate.service.RecommendationService;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<List<UserDto>> handleFindAll() {
        log.debug("Request received: GET /users - retrieving all users");

        List<UserDto> users = userService.findAllUsers();

        log.info("Retrieved {} users successfully", users.size());

        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<UserDto> handleCreateUser(@Valid @RequestBody CreateUserDto newUser) {
        log.debug("Request received: POST /users - creating new user: {}", newUser);

        UserDto createdUser = userService.createUser(newUser);

        log.info("User created successfully. ID: {}, Login: {}", createdUser.getId(), createdUser.getLogin());
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<UserDto> handleUpdateUser(@Valid @RequestBody UpdateUserDto updateUserDto) {
        log.debug("Request received: PUT /users - updating user: {}", updateUserDto);

        UserDto updatedUser = userService.updateUser(updateUserDto);

        log.info("User updated successfully. ID: {}, Login: {}", updatedUser.getId(), updatedUser.getLogin());
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> handleGetUserById(@PathVariable @Positive long id) {
        log.debug("Request received: GET /users/{} - retrieving user by ID", id);

        UserDto user = userService.getUser(id);

        log.info("User retrieved successfully. ID: {}, Login: {}", user.getId(), user.getLogin());
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    // эндпоинт для удаления пользователя
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> handleDeleteUser(@PathVariable @Positive long id) {
        log.debug("Request received: DELETE /users/{} - deleting user", id);

        userService.deleteUser(id);

        log.info("User {} deleted successfully", id);
        return ResponseEntity.noContent().build(); // Возвращаем 204 No Content
    }

    @GetMapping("/{id}/friends")
    public ResponseEntity<List<UserDto>> handleGetUserFriends(@PathVariable long id) {
        log.debug("Request received: GET /users/{}/friends - retrieving friends for user", id);

        List<UserDto> friends = userService.getUserFriends(id);

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
    public ResponseEntity<List<UserDto>> handleCommonFriends(@PathVariable long id, @PathVariable long otherId) {
        log.debug("Request received: GET /users/{}/friends/common/{} - finding common friends", id, otherId);

        List<UserDto> commonFriends = userService.getCommonFriends(id, otherId);

        log.info("Found {} common friends", commonFriends.size());
        return ResponseEntity.ok(commonFriends);
    }

    @GetMapping("/{id}/recommendations")
    public ResponseEntity<List<FilmDto>> getFilmRecommendations(@PathVariable long id) {
        log.debug("Request received: GET /{}/recommendations/ - finding recommendation films for user ID", id);

        List<FilmDto> recommendationFilms = recommendationService.getFilmRecommendationsByUserLikes(id);

        log.info("Found {} recommendation films", recommendationFilms.size());
        return ResponseEntity.ok(recommendationFilms);
    }
}
