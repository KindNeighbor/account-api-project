package project2.Account.execption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import project2.Account.dto.ErrorResponse;

import static project2.Account.type.ErrorCode.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountException.class)
    public ErrorResponse handleAccountException(AccountException e) {
        log.error("{} is occurred.", e.getErrorCode());

        return new ErrorResponse(e.getErrorCode(), e.getErrorMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ErrorResponse handelIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("DataIntegrityViolationException is occurred.", e);

        return new ErrorResponse(INVALID_REQUEST, INVALID_REQUEST.getDescription());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse MethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException is occurred.", e);

        return new ErrorResponse(INVALID_REQUEST, INTERNAL_SERVER_ERROR.getDescription());
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleException(AccountException e) {
        log.error("Exception is occurred.", e);

        return new ErrorResponse(INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR.getDescription());
    }
}
