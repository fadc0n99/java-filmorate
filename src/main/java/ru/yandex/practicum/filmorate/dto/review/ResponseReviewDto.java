package ru.yandex.practicum.filmorate.dto.review;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseReviewDto {
    private Long reviewId;
    private String content;
    private Boolean isPositive;
    private Long userId;
    private Long filmId;
    private Integer useful;
}
