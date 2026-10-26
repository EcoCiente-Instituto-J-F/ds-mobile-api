package br.com.ecociente.mobile.entrypoint.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import br.com.ecociente.mobile.entrypoint.controller.PostagensEmAnaliseController;
import br.com.ecociente.mobile.core.exception.PostagemException;
import br.com.ecociente.mobile.entrypoint.controller.PostagemController;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {
    PostagemController.class,
    PostagensEmAnaliseController.class
})
public class PostagemExceptionHandler {

  @ExceptionHandler(PostagemException.class)
  public ResponseEntity<ErroResponse> tratar(
      PostagemException exception) {
    HttpStatus status = switch (exception.getTipo()) {
      case DADOS_INVALIDOS -> HttpStatus.BAD_REQUEST;
      case NAO_PERMITIDO -> HttpStatus.FORBIDDEN;
      case NAO_ENCONTRADO -> HttpStatus.NOT_FOUND;
      case CONFLITO -> HttpStatus.CONFLICT;
      case INDISPONIVEL -> HttpStatus.SERVICE_UNAVAILABLE;
    };

    return ResponseEntity.status(status).body(
        new ErroResponse(
            status.value(),
            exception.getCodigo(),
            exception.getMessage()));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErroResponse> tratarParametro(
      MethodArgumentTypeMismatchException exception) {
    return ResponseEntity.badRequest().body(
        new ErroResponse(
            400,
            "PARAMETRO_INVALIDO",
            "Valor inválido para o parâmetro: "
                + exception.getName()));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErroResponse> tratarParametroAusente(
      MissingServletRequestParameterException exception) {
    return ResponseEntity.badRequest().body(
        new ErroResponse(
            400,
            "PARAMETRO_INVALIDO",
            "Parâmetro obrigatório ausente: "
                + exception.getParameterName()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErroResponse> tratarValidacao(
      MethodArgumentNotValidException exception) {
    String mensagem = exception.getBindingResult()
        .getAllErrors()
        .stream()
        .findFirst()
        .map(erro -> erro.getDefaultMessage())
        .orElse("Dados inválidos");

    return ResponseEntity.badRequest().body(
        new ErroResponse(
            400,
            "ERRO_VALIDACAO",
            mensagem));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErroResponse> tratarJson(
      HttpMessageNotReadableException exception) {
    return ResponseEntity.badRequest().body(
        new ErroResponse(
            400,
            "JSON_INVALIDO",
            "Corpo da requisição inválido"));
  }

  public record ErroResponse(
      int status,
      String codigo,
      String mensagem) {
  }
}
