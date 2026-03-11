package ru.yandex.practicum.filmorate.dto.mpa;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MpaDto {
    private Long id;
    private String name;
}
