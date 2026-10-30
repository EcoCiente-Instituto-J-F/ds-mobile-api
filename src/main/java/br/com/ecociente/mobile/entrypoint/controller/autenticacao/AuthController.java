package br.com.ecociente.mobile.entrypoint.controller.autenticacao;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.domain.autenticacao.SessaoAutenticada;
import br.com.ecociente.mobile.core.domain.autenticacao.UsuarioCredenciais;
import br.com.ecociente.mobile.core.service.autenticacao.AuthService;

import br.com.ecociente.mobile.entrypoint.dto.autenticacao.request.LoginRequestDto;
import br.com.ecociente.mobile.entrypoint.dto.autenticacao.response.AuthResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.autenticacao.AutenticacaoMapperEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final AutenticacaoMapperEntry autenticacaoMapper;

  @PostMapping("/login")
  @Operation (
    summary = "Autenticação e login de usuário",
    description = "Endpoint pata realizar o login e autenticação de usuários na plataforma com base no seu email e senha."
  )
  @ApiResponses ({
    @ApiResponse (responseCode = "200", description = "Login realizado com sucesso"),
    @ApiResponse (responseCode = "400", description = "Dados inválidos")
  })
  public ResponseEntity<AuthResponseDto> login(@RequestBody @Valid LoginRequestDto request) {
    UsuarioCredenciais credenciais = autenticacaoMapper.toDomain(request);
    SessaoAutenticada sessao = authService.login(credenciais);
    return ResponseEntity.ok(autenticacaoMapper.toResponse(sessao));
  }
}
