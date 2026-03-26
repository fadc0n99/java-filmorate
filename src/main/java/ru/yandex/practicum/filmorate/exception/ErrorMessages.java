package ru.yandex.practicum.filmorate.exception;

import java.time.LocalDate;

public final class ErrorMessages {

    private ErrorMessages() {
    }

    public static final String GENRES_NOT_FOUND = "One or more genres not found";
    public static final String DIRECTORS_NOT_FOUND = "One or more directors not found";
    public static final String RELEASE_DATE_INVALID = "Release date must be after %s";
    public static final String MPA_REQUIRED = "MPA rating is required for film";
    public static final String LIKE_ALREADY_EXISTS = "User has already liked this film";
    public static final String REVIEW_ALREADY_EXISTS = "Review already exists";
    public static final String LIKE_NOT_EXISTS = "User hasn't liked this film";
    public static final String SELF_INTERACTION = "Users cannot interact with themselves";

    public static String userNotFound(long id) {
        return String.format("User with ID %d not found", id);
    }

    public static String filmNotFound(long id) {
        return String.format("Film with ID %d not found", id);
    }

    public static String reviewNotFound(long id) {
        return String.format("Review with ID %d not found", id);
    }

    public static String voteNotFound(long reviewId, long userId) {
        return String.format("Vote not found for review %d and user %d", reviewId, userId);
    }

    public static String mpaNotFound(long id) {
        return String.format("MPA rating with ID %d not found", id);
    }

    public static String genreNotFound(long id) {
        return String.format("Genre with ID %d not found", id);
    }

    public static String notMatchReviewType(long userId, long reviewId, String currentVoteName, String newVoteName) {
        return String.format(
                "User %d has %s on review %d, not %s", userId, currentVoteName, reviewId, newVoteName
        );
    }

    public static String directorNotFound(long id) {
        return String.format("Director with ID %d not found", id);
    }

    public static String releaseDateInvalid(LocalDate minDate) {
        return String.format(RELEASE_DATE_INVALID, minDate);
    }
}