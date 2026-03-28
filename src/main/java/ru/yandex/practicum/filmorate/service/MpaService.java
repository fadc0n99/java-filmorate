package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.mpa.MpaDto;
import ru.yandex.practicum.filmorate.exception.ErrorMessages;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.MpaMapper;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MpaService {

    private final MpaStorage mpaStorage;

    public List<MpaDto> findAllMpa() {
        return mpaStorage.findAll()
                .stream()
                .map(MpaMapper::toDto)
                .toList();
    }

    public MpaDto findMpaById(long id) {
        return mpaStorage.findById(id)
                .map(MpaMapper::toDto)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.mpaNotFound(id)));
    }
}
