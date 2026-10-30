package br.com.ecociente.mobile.entrypoint.exception.calendario;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import br.com.ecociente.mobile.entrypoint.dto.calendario.ErrorResponse;
import br.com.ecociente.mobile.entrypoint.dto.calendario.ValidationError;
import br.com.ecociente.mobile.core.exception.calendario.AgendamentoInvalidoException;
import br.com.ecociente.mobile.core.exception.calendario.AgendamentoNaoEncontradoException;
import br.com.ecociente.mobile.core.exception.calendario.CondominioNaoEncontradoException;
import br.com.ecociente.mobile.core.exception.calendario.CooperativaNaoEncontradaException;
import br.com.ecociente.mobile.core.exception.calendario.PerfilNaoAutorizadoException;

@ControllerAdvice(basePackages = "br.com.ecociente.mobile.entrypoint.controller.calendario")
public class CalendarioExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(CalendarioExceptionHandler.class);

  private ErrorResponse createErrorResponse(HttpStatus status, String codigoError, List<ValidationError> details) {
    return new ErrorResponse(status.value(), codigoError, details);
  }

  @ExceptionHandler(PerfilNaoAutorizadoException.class)
  public ResponseEntity<ErrorResponse> handlePerfilNaoAutorizadoException(PerfilNaoAutorizadoException ex, WebRequest request) {
    log.warn("Perfil não autorizado: {}", ex.getMessage());
    List<ValidationError> details = new ArrayList<>();
    details.add(new ValidationError("perfil", ex.getMessage()));
    var response = createErrorResponse(HttpStatus.FORBIDDEN, "PERFIL_NAO_AUTORIZADO", details);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
  }

  @ExceptionHandler(AgendamentoNaoEncontradoException.class)
  public ResponseEntity<ErrorResponse> handleAgendamentoNaoEncontradoException(AgendamentoNaoEncontradoException ex, WebRequest request) {
    log.warn("Agendamento não encontrado: {}", ex.getMessage());
    List<ValidationError> details = new ArrayList<>();
    details.add(new ValidationError("id", ex.getMessage()));
    var response = createErrorResponse(HttpStatus.NOT_FOUND, "AGENDAMENTO_NAO_ENCONTRADO", details);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(CooperativaNaoEncontradaException.class)
  public ResponseEntity<ErrorResponse> handleCooperativaNaoEncontradaException(CooperativaNaoEncontradaException ex, WebRequest request) {
    log.warn("Cooperativa não encontrada: {}", ex.getMessage());
    List<ValidationError> details = new ArrayList<>();
    details.add(new ValidationError("cooperativaId", ex.getMessage()));
    var response = createErrorResponse(HttpStatus.NOT_FOUND, "COOPERATIVA_NAO_ENCONTRADA", details);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(CondominioNaoEncontradoException.class)
  public ResponseEntity<ErrorResponse> handleCondominioNaoEncontradoException(CondominioNaoEncontradoException ex, WebRequest request) {
    log.warn("Condomínio não encontrado: {}", ex.getMessage());
    List<ValidationError> details = new ArrayList<>();
    details.add(new ValidationError("condominioId", ex.getMessage()));
    var response = createErrorResponse(HttpStatus.NOT_FOUND, "CONDOMINIO_NAO_ENCONTRADO", details);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(AgendamentoInvalidoException.class)
  public ResponseEntity<ErrorResponse> handleAgendamentoInvalidoException(AgendamentoInvalidoException ex, WebRequest request) {
    log.warn("Agendamento inválido: {}", ex.getMessage());
    List<ValidationError> details = new ArrayList<>();
    details.add(new ValidationError("agendamentoId", ex.getMessage()));
    var response = createErrorResponse(HttpStatus.BAD_REQUEST, "AGENDAMENTO_INVALIDO", details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
    log.warn("Tipo inválido para o parâmetro {}: {}", ex.getName(), ex.getValue());
    List<ValidationError> details = new ArrayList<>();
    details.add(new ValidationError(ex.getName(), "Valor inválido para o parâmetro: " + ex.getName() + "'"));
    var response = createErrorResponse(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO", details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
    log.warn("Falha de validação: {}", ex.getMessage());
    List<ValidationError> details = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
        .toList();
    var response = createErrorResponse(HttpStatus.BAD_REQUEST, "ERRO_VALIDACAO", details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, WebRequest request) {
    log.warn("JSON malformado: {}", ex.getMessage());
    List<ValidationError> details = new ArrayList<>();
    details.add(new ValidationError("body", "JSON malformado ou impossível de ler"));
    var response = createErrorResponse(HttpStatus.BAD_REQUEST, "JSON_MALFORMADO", details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
    log.error("Erro inesperado: {}", ex.getMessage(), ex);
    List<ValidationError> details = new ArrayList<>();
    details.add(new ValidationError("erro", "Erro interno do servidor"));
    var response = createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", details);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  @ExceptionHandler(BindException.class)
public ResponseEntity<ErrorResponse> handleBindException(
    BindException ex,
    WebRequest request) {

    log.warn("Parâmetro inválido: {}", ex.getMessage());

    List<ValidationError> details = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> new ValidationError(
            error.getField(),
            "Valor inválido para o parâmetro: " + error.getField()
        ))
        .toList();

    var response = createErrorResponse(
        HttpStatus.BAD_REQUEST,
        "PARAMETRO_INVALIDO",
        details
    );

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(response);
}
}
