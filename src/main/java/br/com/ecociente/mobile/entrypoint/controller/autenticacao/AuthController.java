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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.util.Locale;
import br.com.ecociente.mobile.core.exception.autenticacao.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final AutenticacaoMapperEntry autenticacaoMapper;

    private final FirebaseAuth firebaseAuth;

    public record FirebaseLoginRequest(@NotBlank @Size(max = 8192) String idToken) {}

    @PostMapping("/firebase")
    public ResponseEntity<AuthResponseDto> loginFirebase(@RequestBody @Valid FirebaseLoginRequest request) {
        final FirebaseToken identidade;
        try {
            identidade = firebaseAuth.verifyIdToken(request.idToken(), true);
        } catch (FirebaseAuthException erro) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Firebase inválido ou revogado");
        }

        if (identidade.getEmail() == null || identidade.getEmail().isBlank()
                || !identidade.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "E-mail Firebase não verificado");
        }

        String email = identidade.getEmail().trim().toLowerCase(Locale.ROOT);
        try {
            return ResponseEntity.ok(autenticacaoMapper.toResponse(authService.loginFirebase(email)));
        } catch (RecursoNaoEncontradoException erro) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta não vinculada ao EcoCiente");
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticação e login de usuário", description = "Endpoint pata realizar o login e autenticação de usuários na plataforma com base no seu email e senha.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"), @ApiResponse(responseCode = "400", description = "Dados inválidos") })
    public ResponseEntity<AuthResponseDto> login(@RequestBody @Valid LoginRequestDto request) {
        UsuarioCredenciais credenciais = autenticacaoMapper.toDomain(request);
        SessaoAutenticada sessao = authService.login(credenciais);
        return ResponseEntity.ok(autenticacaoMapper.toResponse(sessao));
    }
}
