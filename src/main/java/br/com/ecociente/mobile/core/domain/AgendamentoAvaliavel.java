package br.com.ecociente.mobile.core.domain;

public record AgendamentoAvaliavel(Integer id, Integer sindicoUsuarioId, boolean realizado) {

    public boolean eDoSindico(Integer usuarioId) {
        return sindicoUsuarioId != null && sindicoUsuarioId.equals(usuarioId);
    }
}
