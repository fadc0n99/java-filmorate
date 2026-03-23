package ru.yandex.practicum.filmorate.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.ErrorResponse;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class ExceptionResponseHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundException(NotFoundException ex) {
        log.warn(ex.getMessage());
        return new ErrorResponse("Not Found", ex.getMessage());
    }

    @ExceptionHandler({ValidationException.class, MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(Exception ex) {
        log.warn("Validation error: {}", ex.getMessage());

        if (ex instanceof MethodArgumentNotValidException validationEx) {
            List<String> errors = validationEx.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(error -> String.format("Field '%s': %s",
                            error.getField(),
                            error.getDefaultMessage()))
                    .toList();

            String message = String.join("; ", errors);
            return new ErrorResponse("Validation Error", message);
        }

        return new ErrorResponse("Validation Error", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleThrowable(final Exception e) {
        log.error("Internal Server Error: {}", e.getMessage());
        return new ErrorResponse("Internal Server Error", e.getMessage());
    }
}