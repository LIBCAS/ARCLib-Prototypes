package cz.cas.lib.arclib.config;

import cz.cas.lib.arclib.exception.BadArgument;
import cz.cas.lib.arclib.exception.ConflictObject;
import cz.cas.lib.arclib.exception.MissingObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * {@link Exception} to HTTP codes mapping.
 * <p>
 * <p>
 * Uses Spring functionality for mapping concrete {@link Exception} onto a returned HTTP code.
 * To create new mapping just create new method with {@link ResponseStatus} and {@link ExceptionHandler}
 * annotations.
 * </p>
 */
@ControllerAdvice
public class ResourceExceptionHandler {

    @ExceptionHandler(MissingObject.class)
    public ResponseEntity missingObject(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.toString());
    }

    @ExceptionHandler(BadArgument.class)
    public ResponseEntity badArgument(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.toString());
    }

    @ExceptionHandler(ConflictObject.class)
    public ResponseEntity conflictException(Exception e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.toString());
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public void bindException() {
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity illegalArgumentException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.toString());
    }
}
