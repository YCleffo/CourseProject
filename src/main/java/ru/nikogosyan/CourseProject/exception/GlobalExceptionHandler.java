package ru.nikogosyan.CourseProject.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException ex, Model model) {
        log.error("Runtime exception: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        log.error("Multipart exception", ex);
        return "error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage());
        log.error("Multipart exception", ex);
        return "access-denied";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, Model model) {
        log.error("File too large: {}", ex.getMessage());
        log.error("Multipart exception", ex);
        model.addAttribute("error", "Файл слишком большой. Максимальный размер: 50MB");
        return "error";
    }

    @ExceptionHandler(MultipartException.class)
    public String handleMultipartException(MultipartException ex, Model model) {
        log.error("Multipart exception: {}", ex.getMessage());
        log.error("Multipart exception", ex);
        model.addAttribute("error", "Ошибка загрузки файла: " + ex.getMessage());
        return "error";
    }
}