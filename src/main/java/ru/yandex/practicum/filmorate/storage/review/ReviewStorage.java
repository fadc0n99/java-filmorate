package ru.yandex.practicum.filmorate.storage.review;

import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.VoteType;

import java.util.List;
import java.util.Optional;

public interface ReviewStorage {
    Review save(Review review);

    Review update(Review review);

    void delete(long id);

    List<Review> findAll(int count);

    List<Review> findByFilmId(long filmId, int limit);

    Optional<Review> findById(long reviewId);

    boolean isExistById(long reviewId);

    void addVote(long id, long userId, VoteType voteType);

    void updateVote(long id, long userId, VoteType voteType);

    void deleteVote(long id, long userId, VoteType voteType);

    void updateReviewUseful(long reviewId);

    Optional<VoteType> findUserVote(long reviewId, long userId);

    boolean existsByUserAndFilm(Long userId, Long filmId);
}
