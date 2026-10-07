package com.techstore.inventario.comun;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import com.techstore.inventario.autenticacion.AutenticacionException;
import com.techstore.inventario.autorizacion.AccesoDenegadoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AutenticacionException.class)
    public ResponseEntity<ProblemDetail> autenticacion(AutenticacionException ex) {
        String titulo = switch (ex.getEstado()) {
            case UNAUTHORIZED -> "No autenticado";
            case LOCKED -> "Cuenta bloqueada";
            default -> "Acceso denegado";
        };
        ProblemDetail problema = problema(ex.getEstado(), titulo, ex.getMessage(), ex.getCodigo());
        ex.getExtras().forEach(problema::setProperty);
        return ResponseEntity.status(ex.getEstado()).body(problema);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ProblemDetail> accesoDenegado(AccesoDenegadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(problema(HttpStatus.FORBIDDEN, "Acceso denegado", ex.getMessage(), ex.getCodigo()));
    }

    @ExceptionHandler(DatosInvalidosException.class)
    public ResponseEntity<ProblemDetail> datosInvalidos(DatosInvalidosException ex) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Datos inválidos", ex.getMessage(), "DATOS_INVALIDOS");
        problema.setProperty("errores", ex.getErrores());
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> noEncontrado(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(problema(HttpStatus.NOT_FOUND, "No encontrado", "Recurso no encontrado", "NO_ENCONTRADO"));
    }

    @ExceptionHandler(ConflictoEstadoException.class)
    public ResponseEntity<ProblemDetail> conflicto(ConflictoEstadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(problema(HttpStatus.CONFLICT, "Conflicto", ex.getMessage(), ex.getCodigo()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> integridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problema(HttpStatus.CONFLICT, "Conflicto",
            "Los datos ya existen o están en uso", "CONFLICTO_DATOS"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> inesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problema(
            HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ocurrió un error inesperado", "ERROR_INTERNO"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, List<String>> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.computeIfAbsent(error.getField(), campo -> new java.util.ArrayList<>())
                .add(error.getDefaultMessage());
        }
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Datos inválidos",
            "Hay datos inválidos en la solicitud", "DATOS_INVALIDOS");
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    public static ProblemDetail problema(HttpStatus estado, String titulo, String detalle, String codigo) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setProperty("codigo", codigo);
        return problema;
    }
}
