package br.com.ecociente.mobile.core.gateway.validador;

import java.util.List;
import java.util.Optional;
import br.com.ecociente.mobile.core.domain.validador.DadosTriagem;
import br.com.ecociente.mobile.core.domain.validador.ResultadoVoto;
import br.com.ecociente.mobile.core.domain.validador.VinculoTrust;

public interface PostagemValidacaoGateway {

    Optional<DadosTriagem> buscarDadosTriagem(int postagemId);

    void gravarTriagem(int postagemId, Boolean aprovada, Double confianca);

    Optional<Integer> buscarUsuarioAutor(int postagemId);

    boolean existePostagem(int postagemId);

    Integer buscarUsuarioSindico(int postagemId);

    ResultadoVoto registrarVoto(int postagemId, int usuarioId, String tipo, Integer motivoDenunciaId, String comentario);

    void registrarDecisao(int postagemId, boolean aprovar);

    List<VinculoTrust> buscarEnvolvidosTrust(int postagemId);

    void atualizarTrustScore(int usuarioId, int condominioId);

    List<Integer> buscarJanelasVencidas(int limite);

    void encerrarJanela(int postagemId);
}
