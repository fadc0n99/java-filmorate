package ru.yandex.practicum.filmorate.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.dto.review.CreateReviewDto;
import ru.yandex.practicum.filmorate.dto.review.ResponseReviewDto;
import ru.yandex.practicum.filmorate.dto.review.UpdateReviewDto;
import ru.yandex.practicum.filmorate.model.Review;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReviewMapper {

    public static Review toEntity(CreateReviewDto dto) {
        return Review.builder()
                .content(dto.getContent())
                .isPositive(dto.getIsPositive())
                .userId(dto.getUserId())
                .filmId(dto.getFilmId())
                .build();
    }

    public static ResponseReviewDto toDto(Review review) {
        return ResponseReviewDto.builder()
                .reviewId(review.getReviewId())
                .content(review.getContent())
                .isPositive(review.getIsPositive())
                .userId(review.getUserId())
                .filmId(review.getFilmId())
                .useful(review.getUseful())
                .build();
    }

    public static Review updateFields(Review review, UpdateReviewDto reviewDto) {
        if (reviewDto.hasContent()) {
            review.setContent(reviewDto.getContent());
        }
        if (reviewDto.hasIsPositive()) {
            review.setIsPositive(reviewDto.getIsPositive());
        }

        return review;
    }
}
