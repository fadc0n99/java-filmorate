package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.dto.mpa.MpaDto;
import ru.yandex.practicum.filmorate.service.MpaService;

import java.util.List;

@RestController
@RequestMapping("/mpa")
@RequiredArgsConstructor
@Slf4j
public class MpaController {

    private final MpaService mpaService;

    @GetMapping
    public ResponseEntity<List<MpaDto>> getMpaRatings() {
        log.debug("Request received: GET /mpa - retrieving all MPA ratings");

        List<MpaDto> mpaDtos = mpaService.findAllMpa();

        log.info("Retrieved {} MPA ratings successfully", mpaDtos.size());
        return ResponseEntity.ok(mpaDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MpaDto> getMpaRatingById(@PathVariable long id) {
        log.debug("Request received: GET /mpa/{} - retrieving MPA rating by ID", id);

        MpaDto mpaDto = mpaService.findMpaById(id);

        log.info("MPA rating retrieved successfully. ID: {}, Name: {}",
                mpaDto.getId(), mpaDto.getName());
        return ResponseEntity.ok(mpaDto);
    }
}
