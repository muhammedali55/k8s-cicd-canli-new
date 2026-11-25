package com.muhammedalihoca.demo.order.web;

import com.muhammedalihoca.demo.order.client.ProductNotFoundException;
import com.muhammedalihoca.demo.order.client.ProductServiceUnavailableException;
import com.muhammedalihoca.demo.order.service.OrderNotFoundException;
import com.muhammedalihoca.demo.order.service.OrderRejectedException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Uygulama hatalarını RFC 9457 ProblemDetail yanıtlarına çevirir.
// ResponseEntityExceptionHandler'dan türediği için doğrulama (400) hataları da ProblemDetail olur.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // @Valid hataları: varsayılan "Invalid request content." yerine hangi alanın neden geçersiz olduğunu yaz
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .sorted()
                .collect(Collectors.joining(", "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Geçersiz istek: " + detail);
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(OrderNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // Stok yetersiz: sipariş REJECTED kaydedildi; id'si "orderId" ek alanında döner
    @ExceptionHandler(OrderRejectedException.class)
    public ProblemDetail handleOrderRejected(OrderRejectedException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setProperty("orderId", e.getOrderId());
        return problem;
    }

    @ExceptionHandler(ProductServiceUnavailableException.class)
    public ProblemDetail handleProductServiceUnavailable(ProductServiceUnavailableException e) {
        log.warn("product-service çağrısı başarısız: {}", e.getCause() != null ? e.getCause().toString() : e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
}
