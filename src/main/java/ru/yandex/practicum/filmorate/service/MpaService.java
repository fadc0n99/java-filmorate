package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.mpa.MpaDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.MpaMapper;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MpaService {

    private final MpaStorage mpaStorage;

    private static final String MPA_NOT_FOUND_MESSAGE = "MPA rating with ID %d not found";

    public List<MpaDto> findAllMpa() {
        return mpaStorage.findAll()
                .stream()
                .map(MpaMapper::mpaToMpaDto)
                .toList();
    }

    public MpaDto findMpaById(long id) {
        return mpaStorage.findById(id)
                .map(MpaMapper::mpaToMpaDto)
                .orElseThrow(() -> new NotFoundException(String.format(MPA_NOT_FOUND_MESSAGE, id)));
    }

    public Set<Mpa> findMpaByIds(Set<Long> mpaIds) {
        return new HashSet<>(mpaStorage.findByIdIn(mpaIds));
    }
}
