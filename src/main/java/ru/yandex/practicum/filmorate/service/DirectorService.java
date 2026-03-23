package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dto.director.DirectorDto;
import ru.yandex.practicum.filmorate.exception.ErrorMessages;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.DirectorMapper;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DirectorService {

    private final DirectorStorage directorStorage;

    public List<Director> findAll() {
        return directorStorage.findAll();
    }

    public Director findById(Integer id) {
        return directorStorage.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.directorNotFound(id)));
    }

    public Director create(DirectorDto directorDto) {
        Director director = DirectorMapper.toEntity(directorDto);

        return directorStorage.create(director);
    }

    public Director update(DirectorDto directorDto) {
        Director director = DirectorMapper.toEntity(directorDto);

        return directorStorage.update(director);
    }

    public void delete(Integer id) {
        validateDirectorExists(id);
        directorStorage.delete(id);
    }

    public void validateDirectorExists(Integer id) {
        if (!directorStorage.isExistById(id)) {
            throw new NotFoundException(ErrorMessages.directorNotFound(id));
        }
    }
}