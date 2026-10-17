package br.com.ecociente.mobile.entrypoint.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecociente.mobile.core.service.UsuarioPerfilService;
import br.com.ecociente.mobile.entrypoint.dto.response.UsuarioPerfilResponseDto;
import br.com.ecociente.mobile.entrypoint.mapper.UsuarioPerfilMapper;

@RestController
public class UsuarioPerfilController {

    private final UsuarioPerfilService service;
    private final UsuarioPerfilMapper mapper;

    public UsuarioPerfilController(
        UsuarioPerfilService service,
        UsuarioPerfilMapper mapper
    ) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/api/usuarios/me")
    public ResponseEntity<UsuarioPerfilResponseDto> buscarMeuPerfil(
        Authentication authentication
    ) {
        if (authentication == null
            || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof Integer usuarioId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return service.buscarMeuPerfil(usuarioId)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}