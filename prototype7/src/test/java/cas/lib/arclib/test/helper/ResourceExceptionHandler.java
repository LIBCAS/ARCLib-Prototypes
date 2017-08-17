package cas.lib.arclib.test.helper;

import cz.inqool.uas.exception.ConflictObject;
import cz.inqool.uas.exception.InvalidAttribute;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * {@link Exception} to HTTP codes mapping.
 *
 * <p>
 *     Uses Spring functionality for mapping concrete {@link Exception} onto a returned HTTP code.
 *     To create new mapping just create new method with {@link ResponseStatus} and {@link ExceptionHandler}
 *     annotations.
 * </p>
 */
@ControllerAdvice
public class ResourceExceptionHandler {

    @ResponseStatus(value= HttpStatus.NOT_FOUND)
    @ExceptionHandler(cz.inqool.uas.exception.MissingObject.class)
    public void missingObject() {}

    @ResponseStatus(value= HttpStatus.NOT_FOUND)
    @ExceptionHandler(cz.inqool.uas.exception.MissingAttribute.class)
    public void missingAttribute() {}

    @ResponseStatus(value= HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidAttribute.class)
    public void invalidAttribute() {}

    @ResponseStatus(value= HttpStatus.BAD_REQUEST)
    @ExceptionHandler(cz.inqool.uas.exception.BadArgument.class)
    public void badArgument() {}

    @ResponseStatus(value= HttpStatus.FORBIDDEN)
    @ExceptionHandler(cz.inqool.uas.exception.ForbiddenObject.class)
    public void forbiddenObject() {}

    @ResponseStatus(value= HttpStatus.FORBIDDEN)
    @ExceptionHandler(cz.inqool.uas.exception.ForbiddenOperation.class)
    public void forbiddenOperation() {}

    @ResponseStatus(value= HttpStatus.CONFLICT)
    @ExceptionHandler(ConflictObject.class)
    public void conflictException() {}

    @ResponseStatus(value= HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public void bindException() {}
}
