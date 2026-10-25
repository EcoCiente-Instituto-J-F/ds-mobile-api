package br.com.ecociente.mobile.entrypoint.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import br.com.ecociente.mobile.core.exception.ChatException;
import br.com.ecociente.mobile.entrypoint.controller.ChatController;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ChatController.class)
public class ChatExceptionHandler {

  @ExceptionHandler(ChatException.class)
  public ResponseEntity<ErroResponse> tratar(
      ChatException exception) {
    return ResponseEntity.status(exception.getStatus())
        .body(new ErroResponse(
            exception.getStatus(),
            exception.getCodigo(),
            exception.getMessage()));
  }

  @ExceptionHandler({
      MethodArgumentNotValidException.class,
      HttpMessageNotReadableException.class,
      MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ErroResponse> tratarDadosInvalidos(
      Exception exception) {
    return ResponseEntity.badRequest().body(
        new ErroResponse(
            400,
            "DADOS_INVALIDOS",
            "Verifique os parâmetros e o corpo da requisição"));
  }

  public record ErroResponse(
      int status,
      String codigo,
      String mensagem) {
  }
}