package br.com.ecociente.mobile.entrypoint.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.ecociente.mobile.core.exception.FluxoQuizException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class FluxoQuizExceptionHandler {

  @ExceptionHandler(FluxoQuizException.class)
  public ResponseEntity<ErroQuizResponse> tratar(
      FluxoQuizException exception) {
    HttpStatus status = switch (exception.getTipo()) {
      case DADOS_INVALIDOS -> HttpStatus.BAD_REQUEST;
      case NAO_PERMITIDO -> HttpStatus.FORBIDDEN;
      case NAO_ENCONTRADO -> HttpStatus.NOT_FOUND;
      case CONFLITO -> HttpStatus.CONFLICT;
    };

    return ResponseEntity.status(status).body(
        new ErroQuizResponse(
            status.value(),
            exception.getCodigo(),
            exception.getMessage()));
  }

  public record ErroQuizResponse(
      int status,
      String codigo,
      String mensagem) {
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErroQuizResponse> tratarValidacao(
      MethodArgumentNotValidException exception) {
    String mensagem = exception.getBindingResult()
        .getAllErrors()
        .stream()
        .findFirst()
        .map(erro -> erro.getDefaultMessage())
        .orElse("Dados inválidos");

    return ResponseEntity.badRequest().body(
        new ErroQuizResponse(
            400,
            "ERRO_VALIDACAO",
            mensagem));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErroQuizResponse> tratarJson(
      HttpMessageNotReadableException exception) {
    return ResponseEntity.badRequest().body(
        new ErroQuizResponse(
            400,
            "JSON_INVALIDO",
            "Corpo da requisição inválido"));
  }
}