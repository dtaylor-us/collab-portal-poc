package com.miso.planning.dpp.web;
import com.miso.planning.dpp.application.*;
import com.miso.planning.dpp.domain.InvalidReviewTransition;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(ReviewNotFound.class) ResponseEntity<ApiError> notFound(RuntimeException e,HttpServletRequest r){return error(HttpStatus.NOT_FOUND,e,r);}
  @ExceptionHandler({InvalidReviewTransition.class,IdempotencyConflict.class}) ResponseEntity<ApiError> conflict(RuntimeException e,HttpServletRequest r){return error(HttpStatus.CONFLICT,e,r);}
  @ExceptionHandler(WorkflowConsistencyException.class) ResponseEntity<ApiError> workflow(RuntimeException e,HttpServletRequest r){return error(HttpStatus.BAD_GATEWAY,e,r);}
  @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class}) ResponseEntity<ApiError> badRequest(Exception e,HttpServletRequest r){return error(HttpStatus.BAD_REQUEST,e,r);}
  private ResponseEntity<ApiError> error(HttpStatus status,Exception e,HttpServletRequest r){return ResponseEntity.status(status).body(new ApiError(Instant.now(),status.value(),status.getReasonPhrase(),e.getMessage(),r.getRequestURI()));}
  public record ApiError(Instant timestamp,int status,String error,String message,String path){}
}
