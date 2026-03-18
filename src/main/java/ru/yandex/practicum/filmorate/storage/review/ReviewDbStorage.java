package ru.yandex.practicum.filmorate.storage.review;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.VoteType;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository("reviewDbStorage")
@Slf4j
public class ReviewDbStorage extends BaseDbStorage<Review> implements ReviewStorage {

    public static final String INSERT_REVIEW = """
            INSERT INTO reviews (content, is_positive, user_id, film_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    public static final String UPDATE_REVIEW = """
            UPDATE reviews
            SET content = ?, is_positive = ?, updated_at = ?
            WHERE review_id = ?
            """;
    public static final String DELETE_REVIEW = "DELETE FROM reviews WHERE review_id = ?";
    public static final String FIND_ALL_WITH_LIMIT = "SELECT * FROM reviews LIMIT ?";
    public static final String FIND_ALL_BY_FILM_ID_WITH_LIMIT = "SELECT * FROM reviews WHERE film_id = ? LIMIT ?";
    public static final String FIND_BY_ID = "SELECT * FROM reviews WHERE review_id = ?";
    public static final String FIND_USER_VOTE = """
            SELECT vote_type FROM review_votes WHERE review_id = ? AND user_id = ?
            """;
    public static final String INSERT_VOTE = """
            INSERT INTO review_votes (review_id, user_id, vote_type, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
            """;
    public static final String UPDATE_VOTE = """
            UPDATE review_votes
            SET vote_type = ?, updated_at = ?
            WHERE review_id = ? AND user_id = ?
            """;
    public static final String DELETE_VOTE = "DELETE FROM review_votes WHERE review_id = ? and user_id = ?";
    public static final String UPDATE_REVIEW_USEFUL = """
            UPDATE reviews r
            SET useful = (
                SELECT COALESCE(
                    SUM(CASE WHEN v.vote_type = 'LIKE' THEN 1 ELSE -1 END),
                    0
                )
                FROM review_votes v
                WHERE v.review_id = r.review_id
            )
            WHERE review_id = ?
            """;

    public ReviewDbStorage(JdbcTemplate jdbc, RowMapper<Review> rowMapper) {
        super(jdbc, rowMapper);
    }

    @Override
    public Review save(Review review) {
        try {
            long id = insert(
                    INSERT_REVIEW,
                    review.getContent(),
                    review.getIsPositive(),
                    review.getUserId(),
                    review.getFilmId(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            review.setReviewId(id);

            return review;
        } catch (DataAccessException e) {
            throw handleReviewError(e, review);
        }
    }

    @Override
    public Review update(Review review) {
        update(
                UPDATE_REVIEW,
                review.getContent(),
                review.getIsPositive(),
                LocalDateTime.now(),
                review.getReviewId()
        );

        return review;
    }

    @Override
    public void delete(long id) {
        update(DELETE_REVIEW, id);
    }

    @Override
    public List<Review> findAll(int count) {
        return findMany(FIND_ALL_WITH_LIMIT, count);
    }

    @Override
    public List<Review> findByFilmId(long filmId, int limit) {
        return findMany(FIND_ALL_BY_FILM_ID_WITH_LIMIT, filmId, limit);
    }

    @Override
    public Optional<Review> findById(long reviewId) {
        return findOne(FIND_BY_ID, reviewId);
    }

    @Override
    public void addVote(long id, long userId, VoteType voteType) {
        try {
            insert(
                    INSERT_VOTE,
                    id,
                    userId,
                    voteType.name(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

        } catch (DataAccessException e) {
            throw handleVoteError(e, id, userId);
        }
    }

    @Override
    public void updateVote(long id, long userId, VoteType voteType) {
        try {
            update(
                    UPDATE_VOTE,
                    voteType.name(),
                    LocalDateTime.now(),
                    id,
                    userId
            );

        } catch (DataAccessException e) {
            throw handleVoteError(e, id, userId);
        }
    }

    @Override
    public void deleteVote(long id, long userId, VoteType voteType) {
        try {
            update(
                    DELETE_VOTE,
                    id,
                    userId
            );

        } catch (DataAccessException e) {
            throw handleVoteError(e, id, userId);
        }
    }

    @Override
    public void updateReviewUseful(long reviewId) {
        update(
                UPDATE_REVIEW_USEFUL,
                reviewId
        );
    }

    @Override
    public Optional<VoteType> findUserVote(long reviewId, long userId) {
        try {
            String voteType = jdbc.queryForObject(FIND_USER_VOTE, String.class, reviewId, userId);
            return Optional.of(VoteType.valueOf(voteType));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private RuntimeException handleReviewError(DataAccessException e, Review review) {
        String message = extractExceptionMessage(e);

        if (message.contains("FOREIGN KEY(USER_ID)")) {
            throw new NotFoundException("User with ID " + review.getUserId() + " not found");
        }
        if (message.contains("FOREIGN KEY(FILM_ID)")) {
            throw new NotFoundException("Film with ID " + review.getFilmId() + " not found");
        }
        if (message.contains("UNIQUE_REVIEW_INDEX")) {
            throw new ValidationException(
                    "Review already exists for film ID " + review.getFilmId() + " and user ID " + review.getUserId());
        }

        throw e;
    }

    private RuntimeException handleVoteError(DataAccessException e, long reviewId, long userId) {
        String message = extractExceptionMessage(e);

        if (message.contains("FOREIGN KEY(USER_ID)")) {
            throw new NotFoundException("User with ID " + userId + " not found");
        }
        if (message.contains("FOREIGN KEY(REVIEW_ID)")) {
            throw new NotFoundException("Review with ID " + reviewId + " not found");
        }
        if (message.contains("UNIQUE_VOTE")) {
            return new ValidationException(
                    String.format("User %d already voted on review %d", userId, reviewId)
            );
        }
        return e;
    }

    private String extractExceptionMessage(DataAccessException e) {
        Throwable rootCause = e.getRootCause();
        if (!(rootCause instanceof SQLIntegrityConstraintViolationException)) {
            throw e;
        }

        return rootCause.getMessage();
    }
}
