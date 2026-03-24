package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.event.EventDto;
import ru.yandex.practicum.filmorate.service.FeedService;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/{id}/feed")
    public ResponseEntity<List<EventDto>> getUserFeed(
            @PathVariable Long id,
            @RequestParam(required = false) @Positive Integer count) {

        log.debug("Request received: GET /users/{}/feed (count={})", id, count);

        List<EventDto> feed = feedService.getUserFeed(id, count);

        log.info("Retrieved {} events for user {}", feed.size(), id);
        return ResponseEntity.ok(feed);
    }
}

