package br.com.ecociente.mobile.core.gateway.senha;

import br.com.ecociente.mobile.core.domain.senha.RecuperacaoSenha;

import java.sql.Timestamp;
import java.util.Optional;

public interface RecuperacaoSenhaGateway {

    RecuperacaoSenha salvar(RecuperacaoSenha recuperacaoSenha);

    Optional<RecuperacaoSenha> buscarUltimaPorUsuarioId(Integer usuarioId);

    void invalidarPendentes(Integer usuarioId, Timestamp agora);
}
