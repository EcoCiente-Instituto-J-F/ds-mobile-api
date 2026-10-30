package br.com.ecociente.mobile.entrypoint.mapper.autenticacao;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.autenticacao.SessaoAutenticada;
import br.com.ecociente.mobile.core.domain.autenticacao.UsuarioCredenciais;
import br.com.ecociente.mobile.entrypoint.dto.autenticacao.request.LoginRequestDto;
import br.com.ecociente.mobile.entrypoint.dto.autenticacao.response.AuthResponseDto;

@Component
public class AutenticacaoMapperEntry {

  public UsuarioCredenciais toDomain(LoginRequestDto requestDto) {
    if (requestDto == null) {
      return null;
    }

    return new UsuarioCredenciais(
        requestDto.email(),
        requestDto.senha());
  }

  public AuthResponseDto toResponse(SessaoAutenticada sessao) {
    if (sessao == null) {
      return null;
    }

    return new AuthResponseDto(
        sessao.getToken(),
        sessao.getTipoToken(),
        sessao.getExpiraEm().longValue(),
        sessao.getUsuarioId(),
        sessao.getNome(),
        sessao.getEmail(),
        sessao.getPerfil());
  }
}
