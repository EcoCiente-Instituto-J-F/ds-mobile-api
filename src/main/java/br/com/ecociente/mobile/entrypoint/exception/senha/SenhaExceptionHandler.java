package br.com.ecociente.mobile.entrypoint.exception.senha;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.ecociente.mobile.core.exception.senha.EnvioEmailException;
import br.com.ecociente.mobile.core.exception.senha.RecursoNaoEncontradoException;
import br.com.ecociente.mobile.core.exception.senha.RegraNegocioException;
import br.com.ecociente.mobile.entrypoint.dto.senha.ErrorResponse;
import br.com.ecociente.mobile.entrypoint.dto.senha.ValidationError;

@RestControllerAdvice(basePackages = "br.com.ecociente.mobile.entrypoint.controller.senha")
public class SenhaExceptionHandler {

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErrorResponse> handleRegraNegocio(RegraNegocioException exception) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .status(400)
                        .codigoError("REGRA_NEGOCIO")
                        .details(List.of(ValidationError.builder()
                                .message(exception.getMessage())
                                .build()))
                        .build());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNaoEncontrado(RecursoNaoEncontradoException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .status(404)
                        .codigoError("RECURSO_NAO_ENCONTRADO")
                        .details(List.of(ValidationError.builder()
                                .message(exception.getMessage())
                                .build()))
                        .build());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.builder()
                        .status(401)
                        .codigoError("CREDENCIAIS_INVALIDAS")
                        .details(List.of(ValidationError.builder()
                                .message("E-mail ou senha inválidos")
                                .build()))
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<ValidationError> erros = exception.getBindingResult().getFieldErrors().stream()
                .map(fe -> ValidationError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .status(400)
                        .codigoError("VALIDACAO")
                        .details(erros)
                        .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonInvalido(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .status(400)
                        .codigoError("VALIDACAO")
                        .details(List.of(ValidationError.builder()
                                .message("JSON inválido")
                                .build()))
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenerico(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .status(500)
                        .codigoError("ERRO_INTERNO")
                        .details(List.of(ValidationError.builder()
                                .message("Erro interno do servidor")
                                .build()))
                        .build());
    }

    @ExceptionHandler(EnvioEmailException.class)
    public ResponseEntity<ErrorResponse> handleEnvioEmail(
            EnvioEmailException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.builder()
                        .status(503)
                        .codigoError("ENVIO_EMAIL_INDISPONIVEL")
                        .details(List.of(ValidationError.builder()
                                .message(
                                        "Não foi possível processar a solicitação. "
                                                + "Tente novamente mais tarde.")
                                .build()))
                        .build());
    }

    @ExceptionHandler(br.com.ecociente.mobile.core.exception.senha.SincronizacaoSenhaException.class)
    public ResponseEntity<ErrorResponse> handleSincronizacaoSenha(
            br.com.ecociente.mobile.core.exception.senha.SincronizacaoSenhaException exception) {

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.builder()
                        .status(503)
                        .codigoError("SINCRONIZACAO_SENHA_INDISPONIVEL")
                        .details(List.of(
                                ValidationError.builder()
                                        .message(
                                                "Não foi possível processar a solicitação. "
                                                        + "Tente novamente mais tarde.")
                                        .build()))
                        .build());
    }

}