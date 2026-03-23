package ru.yandex.practicum.filmorate.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.dto.director.DirectorDto;
import ru.yandex.practicum.filmorate.model.Director;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DirectorMapper {

    public static Director toEntity(DirectorDto dto) {
        return Director.builder()
                .id(dto.getId())
                .name(dto.getName())
                .build();
    }

}
