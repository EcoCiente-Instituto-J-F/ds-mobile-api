package br.com.ecociente.mobile.entrypoint.controller.senha;

import java.sql.Timestamp;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.service.senha.SenhaService;
import br.com.ecociente.mobile.entrypoint.dto.senha.request.AlterarSenhaRequest;
import br.com.ecociente.mobile.entrypoint.dto.senha.request.EsqueciSenhaRequest;
import br.com.ecociente.mobile.entrypoint.dto.senha.request.RedefinirSenhaRequest;
import br.com.ecociente.mobile.entrypoint.dto.senha.response.SenhaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/senhas")
@RequiredArgsConstructor
public class SenhaController {

        private final SenhaService senhaService;

        @PostMapping("/esqueceu")
        public ResponseEntity<SenhaResponse> esqueceuSenha(
                        @Valid @RequestBody EsqueciSenhaRequest request) {

                return resposta(
                                senhaService.solicitarRecuperacao(request.email()));
        }

        @PostMapping("/redefinir")
        public ResponseEntity<SenhaResponse> redefinirSenha(
                        @Valid @RequestBody RedefinirSenhaRequest request) {

                return resposta(
                                senhaService.redefinirSenha(
                                                request.email(),
                                                request.token(),
                                                request.novaSenha(),
                                                request.confirmacaoSenha()));
        }

        @PostMapping("/alterar")
        public ResponseEntity<SenhaResponse> alterarSenha(
                        @Valid @RequestBody AlterarSenhaRequest request,
                        Authentication authentication) {

                return resposta(
                                senhaService.alterarSenha(
                                                usuarioAutenticado(authentication),
                                                request.senhaAtual(),
                                                request.novaSenha(),
                                                request.confirmacaoSenha()));
        }

        private Integer usuarioAutenticado(Authentication authentication) {
                if (authentication == null
                                || !authentication.isAuthenticated()
                                || !(authentication.getPrincipal() instanceof Integer usuarioId)) {
                        throw new BadCredentialsException("Autenticação inválida.");
                }

                return usuarioId;
        }

        private ResponseEntity<SenhaResponse> resposta(Timestamp processadoEm) {
                return ResponseEntity.ok(
                                SenhaResponse.builder()
                                                .processadoEm(processadoEm)
                                                .build());
        }
}