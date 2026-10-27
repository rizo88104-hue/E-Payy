package uz.epay.exception;

import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uz.epay.service.ImageStorage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String SERVER_ERROR = "Server xatosi. Keyinroq urinib ko'ring.";
    static final String VERSION_CONFLICT = "Boshqa joyda o'zgartirildi, sahifani yangilang";

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ProblemDetail> badRequest(BadRequestException e) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> notFound(NotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ProblemDetail> forbidden(ForbiddenException e) {
        return problem(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> conflict(ConflictException e) {
        return problem(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ProblemDetail> tooMany(TooManyRequestsException e) {
        long seconds = Math.max(1, (e.getRetryAfter().toMillis() + 999) / 1000);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds))
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage()));
    }

    @ExceptionHandler({OptimisticLockException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ProblemDetail> optimisticLock(Exception e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, VERSION_CONFLICT);
        pd.setProperty("reason", "version");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> integrity(DataIntegrityViolationException e) {
        log.warn("Ma'lumot yaxlitligi buzildi: {}", e.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, integrityMessage(e));
    }

    static String integrityMessage(DataIntegrityViolationException e) {
        String m = String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase();
        if (m.contains("foreign key") || m.contains("referential")) {
            return "Yozuv boshqa ma'lumotlarda ishlatilgan, uni o'chirib yoki o'zgartirib bo'lmaydi.";
        }
        if (m.contains("unique") || m.contains("duplicate")) {
            return "Bunday yozuv allaqachon mavjud.";
        }
        return "Amalni bajarib bo'lmadi: ma'lumot boshqa yozuvlar bilan to'qnashdi.";
    }

    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void security(RuntimeException e) {
        throw e;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> unexpected(Exception e) {
        String code = UUID.randomUUID().toString();
        log.error("Server xatosi [{}]", code, e);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR);
        pd.setProperty("code", code);
        return ResponseEntity.internalServerError().body(pd);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getAllErrors().stream()
                .map(err -> fieldError(err instanceof FieldError fe ? fe.getField() : err.getObjectName(),
                        err.getDefaultMessage()))
                .toList();
        return validationProblem(ex, headers, request, errors);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(err -> fieldError(err instanceof FieldError fe ? fe.getField()
                                : r.getMethodParameter().getParameterName(), err.getDefaultMessage())))
                .toList();
        return validationProblem(ex, headers, request, errors);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return withDetail(ex, headers, status, request, "So'rov tanasi (JSON) noto'g'ri yoki o'qib bo'lmaydi");
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return withDetail(ex, headers, status, request, "'" + ex.getPropertyName() + "' qiymati noto'g'ri");
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return withDetail(ex, headers, status, request, "'" + ex.getParameterName() + "' parametri majburiy");
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return withDetail(ex, headers, status, request, "Topilmadi: /" + ex.getResourcePath());
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return withDetail(ex, headers, HttpStatus.BAD_REQUEST, request, ImageStorage.TOO_LARGE);
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }

    private static Map<String, String> fieldError(String field, String message) {
        return Map.of("field", field, "message", String.valueOf(message));
    }

    private ResponseEntity<Object> validationProblem(
            Exception ex, HttpHeaders headers, WebRequest request, List<Map<String, String>> errors) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Ma'lumotlar noto'g'ri");
        pd.setProperty("errors", errors);
        return handleExceptionInternal(ex, pd, headers, HttpStatus.BAD_REQUEST, request);
    }

    private ResponseEntity<Object> withDetail(
            Exception ex, HttpHeaders headers, HttpStatusCode status, WebRequest request, String detail) {
        return handleExceptionInternal(ex, ProblemDetail.forStatusAndDetail(status, detail), headers, status, request);
    }
}
