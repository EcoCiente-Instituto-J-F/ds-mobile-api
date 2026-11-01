package br.com.ecociente.mobile.core.service.senha;

import br.com.ecociente.mobile.core.domain.senha.RecuperacaoSenha;
import br.com.ecociente.mobile.core.domain.senha.Usuario;
import br.com.ecociente.mobile.core.exception.senha.RecursoNaoEncontradoException;
import br.com.ecociente.mobile.core.exception.senha.RegraNegocioException;
import br.com.ecociente.mobile.core.gateway.senha.EmailGateway;
import br.com.ecociente.mobile.core.gateway.senha.RecuperacaoSenhaGateway;
import br.com.ecociente.mobile.core.gateway.senha.SenhaEncoderGateway;
import br.com.ecociente.mobile.core.gateway.senha.UsuarioGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.ecociente.mobile.core.gateway.senha.FirebaseSenhaGateway;
import br.com.ecociente.mobile.core.gateway.senha.TentativasRecuperacaoSenhaGateway;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SenhaService {

        private static final int VALIDADE_TOKEN_MINUTOS = 15;

        /** Códigos errados aceitos por e-mail dentro de uma janela de VALIDADE_TOKEN_MINUTOS. */
        static final int MAX_TENTATIVAS_CODIGO = 5;

        private static final SecureRandom SECURE_RANDOM = new SecureRandom();

        private static final Pattern PADRAO_SENHA = Pattern.compile(
                        "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S{8,100}$");

        private static final Pattern PADRAO_TOKEN = Pattern.compile(
                        "^[0-9]{4}$");

        private final UsuarioGateway usuarioGateway;
        private final RecuperacaoSenhaGateway recuperacaoSenhaGateway;
        private final SenhaEncoderGateway senhaEncoderGateway;
        private final EmailGateway emailGateway;
        private final FirebaseSenhaGateway firebaseSenhaGateway;
        private final TentativasRecuperacaoSenhaGateway tentativasGateway;

        @Transactional
        public Timestamp solicitarRecuperacao(String email) {
                var usuarioOptional = usuarioGateway.buscarPorEmail(email);

                if (usuarioOptional.isEmpty()) {
                        return agora();
                }

                Usuario usuario = usuarioOptional.get();
                Timestamp momento = agora();

                recuperacaoSenhaGateway.invalidarPendentes(
                                usuario.getId(),
                                momento);

                String token = gerarToken();

                Timestamp expiraEm = new Timestamp(
                                momento.getTime()
                                                + TimeUnit.MINUTES.toMillis(VALIDADE_TOKEN_MINUTOS));

                RecuperacaoSenha recuperacao = RecuperacaoSenha.builder()
                                .usuarioId(usuario.getId())
                                .tokenHash(senhaEncoderGateway.gerarHash(token))
                                .expiraEm(expiraEm)
                                .utilizadoEm(null)
                                .build();

                recuperacaoSenhaGateway.salvar(recuperacao);

                emailGateway.enviarRecuperacao(usuario.getEmail(), token);

                return agora();
        }

        @Transactional
        public Timestamp redefinirSenha(
                        String email,
                        String token,
                        String novaSenha,
                        String confirmacaoSenha) {
                validarNovaSenha(novaSenha, confirmacaoSenha);

                if (token == null || !PADRAO_TOKEN.matcher(token).matches()) {
                        throw tokenInvalido();
                }

                // O código tem 4 dígitos: sem limite, dá para testar as 10 mil combinações.
                String chaveTentativas = normalizarEmail(email);

                if (tentativasGateway.falhas(chaveTentativas) >= MAX_TENTATIVAS_CODIGO) {
                        throw tentativasEsgotadas();
                }

                Usuario usuario = usuarioGateway.buscarPorEmail(email)
                                .orElseThrow(() -> falhaDeCodigo(chaveTentativas));

                RecuperacaoSenha recuperacao = recuperacaoSenhaGateway
                                .buscarUltimaPorUsuarioId(usuario.getId())
                                .orElseThrow(() -> falhaDeCodigo(chaveTentativas));

                Timestamp momento = agora();

                if (recuperacao.getUtilizadoEm() != null
                                || !recuperacao.getExpiraEm().after(momento)) {
                        throw falhaDeCodigo(chaveTentativas);
                }

                if (!senhaEncoderGateway.corresponde(
                                token,
                                recuperacao.getTokenHash())) {
                        throw falhaDeCodigo(chaveTentativas);
                }

                String senhaHash = senhaEncoderGateway.gerarHash(novaSenha);

                momento = agora();

                if (!recuperacao.getExpiraEm().after(momento)) {
                        throw tokenInvalido();
                }

                usuarioGateway.atualizarSenha(usuario.getId(), senhaHash);

                firebaseSenhaGateway.atualizarSenha(
                                usuario.getEmail(),
                                novaSenha);

                recuperacao.setUtilizadoEm(momento);
                recuperacaoSenhaGateway.salvar(recuperacao);

                recuperacaoSenhaGateway.invalidarPendentes(
                                usuario.getId(),
                                momento);

                tentativasGateway.limpar(chaveTentativas);

                return agora();
        }

        @Transactional
        public Timestamp alterarSenha(
                        Integer usuarioId,
                        String senhaAtual,
                        String novaSenha,
                        String confirmacaoSenha) {

                Usuario usuario = usuarioGateway.buscarAtivoPorId(usuarioId)
                                .orElseThrow(() -> new RecursoNaoEncontradoException(
                                                "Usuário não encontrado ou inativo."));

                validarNovaSenha(novaSenha, confirmacaoSenha);

                if (!senhaEncoderGateway.corresponde(
                                senhaAtual,
                                usuario.getSenha())) {
                        throw new RegraNegocioException(
                                        "A senha atual está incorreta.");
                }

                String senhaHash = senhaEncoderGateway.gerarHash(novaSenha);

                usuarioGateway.atualizarSenha(usuario.getId(), senhaHash);

                firebaseSenhaGateway.atualizarSenha(
                                usuario.getEmail(),
                                novaSenha);

                recuperacaoSenhaGateway.invalidarPendentes(
                                usuario.getId(),
                                agora());

                return agora();
        }

        private void validarNovaSenha(
                        String novaSenha,
                        String confirmacaoSenha) {
                if (novaSenha == null
                                || !PADRAO_SENHA.matcher(novaSenha).matches()) {
                        throw new RegraNegocioException(
                                        "A senha deve ter entre 8 e 100 caracteres, "
                                                        + "incluindo uma letra maiúscula, "
                                                        + "uma letra minúscula, um número "
                                                        + "e um caractere especial, sem espaços.");
                }

                if (!novaSenha.equals(confirmacaoSenha)) {
                        throw new RegraNegocioException(
                                        "A nova senha e a confirmação devem ser iguais.");
                }
        }

        private String gerarToken() {
                return String.format(
                                Locale.ROOT,
                                "%04d",
                                SECURE_RANDOM.nextInt(10000));
        }

        private RegraNegocioException tokenInvalido() {
                return new RegraNegocioException(
                                "Código inválido, expirado ou já utilizado.");
        }

        /**
         * Conta a falha antes de responder. A contagem fica no Redis e não volta no rollback da
         * transação, por isso o bloqueio vale mesmo com a exceção.
         */
        private RegraNegocioException falhaDeCodigo(String chaveTentativas) {
                tentativasGateway.registrarFalha(
                                chaveTentativas,
                                Duration.ofMinutes(VALIDADE_TOKEN_MINUTOS));
                return tokenInvalido();
        }

        private RegraNegocioException tentativasEsgotadas() {
                return new RegraNegocioException(
                                "Muitas tentativas com código inválido. Aguarde "
                                                + VALIDADE_TOKEN_MINUTOS
                                                + " minutos e solicite um novo código.");
        }

        private static String normalizarEmail(String email) {
                return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        }

        private Timestamp agora() {
                return new Timestamp(System.currentTimeMillis());
        }
}