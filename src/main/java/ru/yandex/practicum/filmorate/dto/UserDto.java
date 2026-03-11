package ru.yandex.practicum.filmorate.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UserDto {
    private long id;
    private String login;
    private String name;
    private String email;
    private LocalDate birthday;
}

