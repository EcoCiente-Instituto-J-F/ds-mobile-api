package br.com.ecociente.mobile.entrypoint.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import br.com.ecociente.mobile.core.exception.ConsultaCooperativaException;
import br.com.ecociente.mobile.entrypoint.controller.AvaliacoesCooperativaController;
import br.com.ecociente.mobile.entrypoint.controller.CooperativaController;
import br.com.ecociente.mobile.entrypoint.controller.CooperativaDetalheController;
import br.com.ecociente.mobile.entrypoint.controller.CondominiosAtendidosController;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {
    CondominiosAtendidosController.class,
    AvaliacoesCooperativaController.class,
    CooperativaController.class,
    CooperativaDetalheController.class
})
public class ConsultaCooperativaExceptionHandler {

  @ExceptionHandler(ConsultaCooperativaException.class)
  public ResponseEntity<ErroResponse> tratar(
      ConsultaCooperativaException exception) {
    HttpStatus status = switch (exception.getTipo()) {
      case DADOS_INVALIDOS -> HttpStatus.BAD_REQUEST;
      case NAO_PERMITIDO -> HttpStatus.FORBIDDEN;
      case NAO_ENCONTRADO -> HttpStatus.NOT_FOUND;
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

  public record ErroResponse(
      int status,
      String codigo,
      String mensagem) {
  }
}