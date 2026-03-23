package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.director.DirectorDto;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.service.DirectorService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/directors")
@RequiredArgsConstructor
public class DirectorController {

    private final DirectorService directorService;

    @GetMapping
    public List<Director> findAll() {
        log.info("Request received: GET /directors - retrieving all directors");
        return directorService.findAll();
    }

    @GetMapping("/{id}")
    public Director findById(@PathVariable Integer id) {
        log.info("Request received: GET /directors/{} - retrieving director by ID", id);
        return directorService.findById(id);
    }

    @PostMapping
    public Director create(@Valid @RequestBody DirectorDto director) {
        log.info("Request received: POST /directors - retrieving create director");
        return directorService.create(director);
    }

    @PutMapping
    public Director update(@Valid @RequestBody DirectorDto director) {
        log.info("Request received: PUT /directors - retrieving update director");
        return directorService.update(director);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        log.info("Request received: DELETE /directors/{} - retrieving delete director by ID", id);
        directorService.delete(id);
    }
}