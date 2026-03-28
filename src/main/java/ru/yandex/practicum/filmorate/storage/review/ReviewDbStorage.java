package ru.yandex.practicum.filmorate.storage.review;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.VoteType;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;

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
    public static final String FIND_ALL_WITH_LIMIT = "SELECT * FROM reviews ORDER BY useful DESC LIMIT ?";
    public static final String FIND_ALL_BY_FILM_ID_WITH_LIMIT =
            "SELECT * FROM reviews WHERE film_id = ? ORDER BY useful DESC LIMIT ?";
    public static final String FIND_BY_ID = "SELECT * FROM reviews WHERE review_id = ?";
    public static final String FIND_USER_VOTE = """
            SELECT vote_type FROM review_votes WHERE review_id = ? AND user_id = ?
            """;
    private static final String EXISTS_BY_ID = "SELECT EXISTS(SELECT 1 FROM reviews WHERE review_id = ?)";
    private static final String EXISTS_BY_USER_AND_FILM =
            "SELECT EXISTS(SELECT 1 FROM reviews WHERE user_id = ? and film_id = ?)";
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
            ), updated_at = ?
            WHERE review_id = ?
            """;

    public ReviewDbStorage(JdbcTemplate jdbc,
                           NamedParameterJdbcTemplate namedJdbc,
                           RowMapper<Review> rowMapper) {
        super(jdbc, namedJdbc, rowMapper);
    }

    @Override
    public Review save(Review review) {
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
    public boolean isExistById(long reviewId) {
        return exists(EXISTS_BY_ID, reviewId);
    }

    @Override
    public void addVote(long id, long userId, VoteType voteType) {
        insert(
                INSERT_VOTE,
                id,
                userId,
                voteType.name(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Override
    public void updateVote(long id, long userId, VoteType voteType) {
        update(UPDATE_VOTE, voteType.name(), LocalDateTime.now(), id, userId);
    }

    @Override
    public void deleteVote(long id, long userId, VoteType voteType) {
        update(DELETE_VOTE, id, userId);
    }

    @Override
    public void updateReviewUseful(long reviewId) {
        update(
                UPDATE_REVIEW_USEFUL,
                LocalDateTime.now(),
                reviewId
        );
    }

    @Override
    public Optional<VoteType> findUserVote(long reviewId, long userId) {
        return findOne(FIND_USER_VOTE, String.class, reviewId, userId)
                .map(VoteType::valueOf);
    }

    @Override
    public boolean existsByUserAndFilm(Long userId, Long filmId) {
        return exists(EXISTS_BY_USER_AND_FILM, userId, filmId);
    }
}
