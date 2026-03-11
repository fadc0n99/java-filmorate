package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateUserDto {
    @NotNull
    @NotBlank
    @Pattern(regexp = "^\\S+$")
    private String login;

    private String name;

    @NotNull
    @NotBlank
    @Email
    private String email;

    @PastOrPresent
    private LocalDate birthday;
}
