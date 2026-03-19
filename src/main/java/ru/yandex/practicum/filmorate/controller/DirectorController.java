package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
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
        log.info("Получен запрос GET /directors");
        return directorService.findAll();
    }

    @GetMapping("/{id}")
    public Director findById(@PathVariable Integer id) {
        log.info("Получен запрос GET /directors/{}", id);
        return directorService.findById(id);
    }

    @PostMapping
    public Director create(@Valid @RequestBody Director director) {
        log.info("Получен запрос POST /directors с телом: {}", director);
        return directorService.create(director);
    }

    @PutMapping
    public Director update(@Valid @RequestBody Director director) {
        log.info("Получен запрос PUT /directors с телом: {}", director);
        return directorService.update(director);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        log.info("Получен запрос DELETE /directors/{}", id);
        directorService.delete(id);
    }
}