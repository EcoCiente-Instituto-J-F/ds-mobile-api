package br.com.ecociente.mobile.core.domain.validador;

/** Usuário envolvido numa postagem que tem vínculo com o condomínio (quem tem trust score a atualizar). */
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record VinculoTrust(int condominioId, int usuarioId) {
}
