package ru.yandex.practicum.filmorate.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.dto.user.CreateUserDto;
import ru.yandex.practicum.filmorate.dto.user.UpdateUserDto;
import ru.yandex.practicum.filmorate.dto.user.UserDto;
import ru.yandex.practicum.filmorate.model.User;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserMapper {

    public static User mapToUser(CreateUserDto dto) {
        return User.builder()
                .login(dto.getLogin())
                .name(dto.getName())
                .email(dto.getEmail())
                .birthday(dto.getBirthday())
                .build();
    }

    public static UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .login(user.getLogin())
                .name(user.getName())
                .email(user.getEmail())
                .birthday(user.getBirthday())
                .build();
    }

    public static User updateUserFields(User user, UpdateUserDto dto) {
        if (dto.hasLogin()) {
            user.setLogin(dto.getLogin());
        }
        if (dto.hasName()) {
            user.setName(dto.getName());
        }
        if (dto.hasEmail()) {
            user.setEmail(dto.getEmail());
        }
        if (dto.hasBirthday()) {
            user.setBirthday(dto.getBirthday());
        }

        return user;
    }
}
