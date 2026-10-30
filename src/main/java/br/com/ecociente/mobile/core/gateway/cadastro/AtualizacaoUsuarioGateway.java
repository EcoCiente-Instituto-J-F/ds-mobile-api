package br.com.ecociente.mobile.core.gateway.cadastro;

import java.util.List;
import java.util.Optional;

import br.com.ecociente.mobile.core.domain.cadastro.AtualizacaoCondominio;
import br.com.ecociente.mobile.core.domain.cadastro.AtualizacaoDados;
import br.com.ecociente.mobile.core.domain.cadastro.Endereco;
import br.com.ecociente.mobile.core.domain.cadastro.UsuarioEdicao;

public interface AtualizacaoUsuarioGateway {

    Optional<UsuarioEdicao> buscarUsuarioAtivo(Integer usuarioId);

    boolean emailPertenceAOutroUsuario(
        String email,
        Integer usuarioId
    );

    void atualizarDados(
        Integer usuarioId,
        AtualizacaoDados dados
    );

    List<Integer> buscarTelefonesAtivos(Integer usuarioId);

    void criarTelefone(Integer usuarioId, String numero);

    void atualizarTelefone(
        Integer usuarioId,
        Integer telefoneId,
        String numero
    );

    Optional<Integer> buscarEnderecoCooperativa(Integer usuarioId);

    boolean enderecoExclusivoDoUsuario(
        Integer usuarioId,
        Integer enderecoId
    );

    Optional<Integer> buscarEnderecoCondominioDoSindico(
        Integer usuarioId,
        Integer condominioId
    );

    void atualizarEndereco(Endereco endereco);

    boolean condominioAtivoDoSindico(
        Integer usuarioId,
        Integer condominioId
    );

    boolean tipoCondominioExiste(Integer tipoCondominioId);

    void atualizarCondominio(
        Integer condominioId,
        AtualizacaoCondominio dados
    );

    void desativarUsuario(Integer usuarioId);
}