package br.com.ecociente.mobile.core.service.pontuacao;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.pontuacao.Postagem;
import br.com.ecociente.mobile.core.exception.pontuacao.BusinessException;
import br.com.ecociente.mobile.core.exception.pontuacao.NotFoundException;
import br.com.ecociente.mobile.core.gateway.pontuacao.ConfiancaPostagemGateway;
import br.com.ecociente.mobile.core.gateway.pontuacao.PostagemGateway;
import br.com.ecociente.mobile.core.gateway.pontuacao.PostagemValidacaoGateway;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostagemValidacaoService {

    private final PostagemValidacaoGateway validacaoGateway;
    private final PostagemGateway postagemGateway;
    private final PontuacaoService pontuacaoService;
    private final ConfiancaPostagemGateway confiancaGateway;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void encerrar(Integer postagemId) {
        if (!validacaoGateway.tentarBloquearFechamento(postagemId)) {
            throw new BusinessException(
                    "FECHAMENTO_EM_PROCESSAMENTO",
                    "O fechamento desta postagem já está em processamento");
        }

        var postagem = bloquearPostagem(postagemId);

        if (postagem.getResolvidoEm() != null) {
            return;
        }

        if (!validacaoGateway.janelaVencida(postagemId)) {
            throw new BusinessException(
                    "JANELA_AINDA_ABERTA",
                    "A janela comunitária desta postagem ainda não terminou");
        }

        validacaoGateway.encerrarJanela(postagemId);

        atualizarPontuacao(postagemId);
        confiancaGateway.atualizarEnvolvidos(postagemId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void decidir(Integer postagemId, boolean aprovar) {
        bloquearPostagem(postagemId);

        validacaoGateway.decidir(postagemId, aprovar);

        atualizarPontuacao(postagemId);
        confiancaGateway.atualizarEnvolvidos(postagemId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Integer prepararVoto(Integer postagemId) {
        return bloquearPostagem(postagemId).getUsuarioId();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reconciliarVoto(Integer postagemId) {
        // Ajusta um crédito existente; não cria crédito antecipado.
        pontuacaoService.reconciliarPostagem(
                postagemId,
                "voto-" + postagemId + "-" + UUID.randomUUID());
    }

    private void atualizarPontuacao(Integer postagemId) {
        var atualizada = bloquearPostagem(postagemId);

        if (Boolean.TRUE.equals(atualizada.getPontuacaoAtiva())) {
            pontuacaoService.consolidarPostagem(
                    postagemId,
                    "postagem-" + postagemId);
        }

        pontuacaoService.reconciliarPostagem(
                postagemId,
                "ajuste-" + postagemId + "-" + UUID.randomUUID());
    }

    private Postagem bloquearPostagem(Integer postagemId) {
        Integer usuarioId = postagemGateway.buscarUsuarioId(postagemId)
                .orElseThrow(() -> new NotFoundException(
                        "POSTAGEM_NAO_ENCONTRADA",
                        "Postagem não encontrada"));

        if (!postagemGateway.bloquearUsuario(usuarioId)) {
            throw new NotFoundException(
                    "USUARIO_NAO_ENCONTRADO",
                    "Usuário da postagem não encontrado");
        }

        var postagem = postagemGateway.buscarComBloqueio(postagemId)
                .orElseThrow(() -> new NotFoundException(
                        "POSTAGEM_NAO_ENCONTRADA",
                        "Postagem não encontrada"));

        if (!usuarioId.equals(postagem.getUsuarioId())) {
            throw new BusinessException(
                    "POSTAGEM_ALTERADA",
                    "A postagem foi alterada. Tente novamente");
        }

        return postagem;
    }
}