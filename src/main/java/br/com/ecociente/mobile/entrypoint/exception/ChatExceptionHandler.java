package br.com.ecociente.mobile.entrypoint.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import br.com.ecociente.mobile.core.exception.ChatException;
import br.com.ecociente.mobile.entrypoint.controller.ChatController;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ChatController.class)
public class ChatExceptionHandler {
  @ExceptionHandler(ChatException.class)
  public ResponseEntity<Erro> tratar(ChatException e) {
    return ResponseEntity.status(e.getStatus()).body(new Erro(e.getStatus(), e.getCodigo(), e.getMessage()));
  }
  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
      MethodArgumentTypeMismatchException.class})
  public ResponseEntity<Erro> invalido(Exception e) {
    return ResponseEntity.badRequest().body(new Erro(400, "DADOS_INVALIDOS", "Verifique os parâmetros e o corpo da requisição"));
  }
  public record Erro(int status, String codigo, String mensagem) {}
}
