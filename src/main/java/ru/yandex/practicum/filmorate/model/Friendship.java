package ru.yandex.practicum.filmorate.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class Friendship {
    private Long id;
    private Long userId;
    private Long friendId;
    private LocalDate createdAt;
}
