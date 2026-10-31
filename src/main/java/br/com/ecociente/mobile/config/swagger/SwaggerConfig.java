package br.com.ecociente.mobile.config.swagger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

/**
 * Documentação do contrato HTTP da API unificada.
 * A configuração não altera autorizações nem regras de negócio.
 */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
    info = @Info(
        title = "EcoCiente — API Mobile",
        version = "1.1.0",
        description = """
            API unificada de autenticação, cadastro, recuperação de senha,
            educação, calendário, chat, descartes, validação e pontuação.

            Autentique-se em POST /auth/login. No botão Authorize,
            informe somente o token JWT, sem o prefixo Bearer.

            Os IDs são numéricos e devem existir no ambiente utilizado.
            Datas de nascimento usam yyyy-MM-dd. Datas com fuso usam ISO 8601.
            Exemplos são ilustrativos e não representam registros cadastrados.

            A paginação segue os parâmetros de cada operação:
            page/size ou pagina/tamanho. A primeira página é zero.
            Operações administrativas dependem das permissões do usuário.
            """,
        contact = @Contact(name = "Equipe EcoCiente")),
    servers = @Server(url = "/", description = "Servidor atual"),
    security = @SecurityRequirement(name = SwaggerConfig.ESQUEMA_BEARER))
@SecurityScheme(
    name = SwaggerConfig.ESQUEMA_BEARER,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class SwaggerConfig {

    public static final String ESQUEMA_BEARER = "bearerAuth";

    private static final Map<String, Documento> DOCUMENTOS = documentos();

    @Bean
    public OperationCustomizer documentacaoEcociente() {
        return (operation, handler) -> {
            String chave = handler.getBeanType().getSimpleName()
                    + "#" + handler.getMethod().getName();

            Documento documento = DOCUMENTOS.get(chave);

            if (documento == null) {
                return operation;
            }

            operation.setOperationId(chave.replace("#", "_"));
            operation.setTags(List.of(documento.tag()));
            operation.setSummary(documento.resumo());
            operation.setDescription(documento.descricao());

            boolean publica =
                    chave.equals("AuthController#login")
                    || chave.startsWith("CadastroController#")
                    || chave.equals("SenhaController#esqueceuSenha")
                    || chave.equals("SenhaController#redefinirSenha");

            operation.setSecurity(
                    publica
                        ? new ArrayList<>()
                        : List.of(
                            new io.swagger.v3.oas.models.security.SecurityRequirement()
                                .addList(ESQUEMA_BEARER)));

            if (operation.getResponses() == null) {
                operation.setResponses(new ApiResponses());
            }

            ApiResponses respostas = operation.getResponses();

            // Preserva o schema de sucesso inferido pelo springdoc.
            ApiResponse sucesso = respostas.get(documento.status());

            if (sucesso == null) {
                sucesso = respostas.entrySet().stream()
                        .filter(e -> e.getKey().matches("2\\d\\d"))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(new ApiResponse());
            }

            respostas.keySet().removeIf(codigo -> codigo.matches("2\\d\\d"));
            sucesso.setDescription(
                    documento.status().equals("204")
                        ? "Operação concluída, sem corpo de resposta."
                        : "Operação concluída com sucesso.");

            if (documento.status().equals("204")) {
                sucesso.setContent(null);
            }

            respostas.addApiResponse(documento.status(), sucesso);

            if (!publica || chave.equals("AuthController#login")) {
                respostas.putIfAbsent(
                        "401",
                        new ApiResponse().description(
                            "Autenticação ausente, inválida ou credenciais recusadas."));
            }

            if (operation.getParameters() != null) {
                for (var parametro : operation.getParameters()) {
                    String nome = parametro.getName();

                    if ("Idempotency-Key".equalsIgnoreCase(nome)) {
                        parametro.setDescription(
                            "Chave da operação, com até 150 caracteres. "
                            + "Reutilize a mesma chave no retry da mesma operação; "
                            + "não a reutilize para outro recurso.");
                        parametro.setExample("operacao-exemplo-001");
                    } else if ("page".equals(nome) || "pagina".equals(nome)) {
                        parametro.setDescription("Índice da página, começando em zero.");
                    } else if ("size".equals(nome) || "tamanho".equals(nome)) {
                        parametro.setDescription(
                            "Quantidade de registros por página. "
                            + "Consulte as restrições desta operação.");
                    } else if ("sort".equals(nome)) {
                        parametro.setDescription(
                            "Ordenação no formato campo,asc ou campo,desc. "
                            + "Os campos aceitos dependem da consulta.");
                    }
                }
            }

            // Estes endpoints rejeitam qualquer corpo, mesmo byte[].
            if (chave.startsWith("PontuacaoController#")
                    && (chave.contains("#consolidar")
                        || chave.endsWith("#reconciliarPostagem"))) {
                operation.setRequestBody(null);
            }

            return operation;
        };
    }

    private static Map<String, Documento> documentos() {
        Map<String, Documento> mapa = new LinkedHashMap<>();

        String catalogo = """
AuthController#login|Autenticação|Autenticar usuário|200|Valida e-mail e senha e retorna o JWT e os dados da sessão. Não exige token anterior.
CadastroController#cadastroComum|Cadastro|Cadastrar usuário comum|201|Cadastra usuário e endereço. E-mail e CPF devem estar disponíveis. A data de nascimento usa yyyy-MM-dd.
CadastroController#cadastrarMorador|Cadastro|Cadastrar morador|201|Cadastra o morador e seu vínculo por código de condomínio. Campos não permitidos são recusados.
CadastroController#cadastrarCooperativa|Cadastro|Cadastrar cooperativa|201|Cadastra responsável, cooperativa e endereço. O CNPJ é validado e consultado externamente.
AtualizacaoUsuarioController#atualizarDados|Perfil|Atualizar dados pessoais|204|Atualiza somente os campos permitidos do próprio usuário. Omissão ou null preservam os dados pessoais; avatar vazio remove a imagem. CPF, senha e tipo não são editados aqui.
AtualizacaoUsuarioController#atualizarMeuEndereco|Perfil|Atualizar meu endereço|204|Disponível para usuário comum e cooperativa conforme vínculo do endereço. O usuário comum não pode alterar endereço compartilhado.
AtualizacaoUsuarioController#atualizarEnderecoCondominio|Condomínios|Atualizar endereço do condomínio|204|Exige o síndico responsável pelo condomínio. Consulta o CEP e preserva o identificador do endereço.
AtualizacaoUsuarioController#atualizarCondominio|Condomínios|Atualizar condomínio|204|Atualiza nome, tipo e foto do condomínio ativo sob responsabilidade do síndico autenticado.
AtualizacaoUsuarioController#excluirConta|Perfil|Desativar minha conta|204|Desativa logicamente o usuário autenticado.
UsuarioPerfilController#buscarMeuPerfil|Perfil|Consultar meu perfil|200|Retorna os dados e vínculos disponíveis do usuário identificado pelo JWT.
SenhaController#esqueceuSenha|Senha|Solicitar código de recuperação|200|Envia código de quatro dígitos por e-mail quando o usuário existe. A resposta não revela se o e-mail está cadastrado. O código expira em 15 minutos.
SenhaController#redefinirSenha|Senha|Redefinir senha com código|200|Valida e-mail, código e confirmação. O código é de uso único. Atualiza a senha local e sincroniza com Firebase conforme a integração.
SenhaController#alterarSenha|Senha|Alterar minha senha|200|Exige JWT, usuário ativo, senha atual correta e confirmação da nova senha. Invalida códigos pendentes e sincroniza com Firebase.
CursoController#listarCursos|Educação|Listar cursos ativos|200|Consulta os cursos disponíveis ao aplicativo.
CursoController#buscarCurso|Educação|Consultar curso|200|Consulta um curso disponível pelo ID.
AulaController#listarAulas|Educação|Listar aulas do curso|200|Consulta as aulas disponíveis do curso informado.
AulaController#buscarAula|Educação|Consultar aula|200|Consulta uma aula disponível pelo ID.
QuizController#buscarQuizDaAula|Educação|Consultar quiz da aula|200|Consulta o quiz disponível para a aula informada.
TentativaQuizController#iniciar|Quizzes|Iniciar tentativa|201|Cria uma tentativa para usuário comum ou morador e devolve as perguntas sem expor o gabarito.
TentativaQuizController#finalizar|Quizzes|Finalizar e corrigir tentativa|200|Recebe todas as respostas, corrige e registra o resultado. Somente o dono pode finalizar. Tentativas aprovadas seguem para consolidação de pontos; consulte o resultado de pontuação na resposta.
TentativaQuizController#revisar|Quizzes|Consultar revisão da tentativa|200|Disponível ao dono após a conclusão. Retorna respostas escolhidas, corretas e explicações disponíveis.
TentativaQuizController#historico|Quizzes|Consultar meu histórico|200|Lista tentativas concluídas do usuário autenticado, respeitando o limite solicitado.
TentativaQuizController#quizzesConcluidos|Quizzes|Listar IDs de quizzes concluídos|200|Retorna os quizzes concluídos pelo usuário, incluindo tentativas reprovadas e sem repetir o mesmo quiz.
AgendamentoController#listarAgendamentos|Calendário|Listar agendamentos|200|Aplica filtros e paginação conforme o perfil e os vínculos do usuário. Síndico, cooperativa e morador consultam somente os agendamentos autorizados.
AgendamentoController#buscarProximoAgendamento|Calendário|Consultar próximo agendamento|200|Consulta o próximo agendamento autorizado conforme filtros. Retorna 404 quando não houver resultado.
ColetaController#criar|Calendário|Solicitar coleta|201|Cria uma solicitação pelo representante do condomínio ou da cooperativa. A data inicial deve estar no futuro; o mesmo usuário não pode representar as duas partes.
ColetaController#atualizarStatus|Calendário|Atualizar status da coleta|200|A resposta depende da parte destinatária e da transição permitida. Somente a cooperativa envolvida pode marcar uma coleta confirmada como realizada.
AvaliacaoColetaController#avaliar|Coletas|Avaliar coleta|201|Registra nota e comentário da coleta conforme as permissões e o estado do agendamento.
AvaliacoesCooperativaController#consultar|Cooperativas|Consultar avaliações da cooperativa|200|Retorna resumo e avaliações paginadas conforme o acesso do usuário autenticado.
CondominiosAtendidosController#consultar|Cooperativas|Listar condomínios atendidos|200|Consulta os condomínios associados ao atendimento da cooperativa, com paginação.
CooperativaController#listarProximas|Cooperativas|Consultar cooperativas próximas|200|Consulta cooperativas pela localização disponível no contexto do usuário. Não recebe coordenadas GPS.
CooperativaDetalheController#consultar|Cooperativas|Consultar detalhes da cooperativa|200|Consulta os dados de uma cooperativa conforme as permissões do usuário.
DashboardSindicoController#consultar|Condomínios|Consultar dashboard do síndico|200|Retorna os indicadores do condomínio sob responsabilidade do síndico autenticado.
PostagensEmAnaliseController#consultar|Descartes|Listar postagens em análise|200|Lista postagens pendentes do condomínio autorizado, com paginação.
CategoriaResiduoController#listar|Descartes|Listar categorias de resíduos|200|Retorna categorias recicláveis e seus pontos base. A pontuação efetiva também depende das regras e limites da consolidação.
PostagemController#criar|Descartes|Registrar descarte|201|Registra a foto e o contexto do morador aprovado e executa a triagem. O hash da foto deve ser único. Pontos dependem da validação e consolidação.
PostagemController#votar|Descartes|Votar na postagem|200|Aprova ou denuncia usando o usuário do JWT. A denúncia exige motivo; o autor não pode votar na própria postagem.
PostagemController#listarDoCondominio|Descartes|Consultar feed do condomínio|200|Lista descartes do condomínio autorizado, dos mais recentes aos mais antigos.
PostagemController#listarMinhas|Descartes|Consultar meus descartes|200|Lista somente as postagens do usuário autenticado, com paginação.
ChatController#criar|Chat|Criar ou obter conversa|200|Obtém a conversa entre condomínio e cooperativa conforme a participação do usuário. A primeira mensagem deve ser enviada pela cooperativa.
ChatController#listar|Chat|Listar conversas|200|Lista conversas do participante, filtrando arquivadas e aplicando paginação.
ChatController#enviar|Chat|Enviar mensagem|201|Envia texto para uma conversa autorizada. A primeira mensagem deve ser da cooperativa. Limite de 2000 caracteres.
ChatController#mensagens|Chat|Listar mensagens|200|Consulta mensagens com paginação. A listagem não marca leitura automaticamente.
ChatController#ler|Chat|Marcar mensagens recebidas como lidas|200|Marca somente mensagens recebidas até o ID informado, inclusive. Sem corpo ou sem limite, marca todas as recebidas. Preserva a data de leituras anteriores.
ChatController#arquivar|Chat|Arquivar ou desarquivar conversa|204|Altera o arquivamento compartilhado da conversa, preservando o histórico.
PontuacaoController#consolidarPostagem|Pontuação|Consolidar pontos do descarte|200|Operação administrativa para ADMIN ou SERVICE. Exige Idempotency-Key e não aceita corpo. Respeita a validação da postagem e os limites de pontuação.
PontuacaoController#consolidarQuiz|Pontuação|Consolidar pontos da tentativa|200|Operação administrativa para ADMIN ou SERVICE. Exige tentativa concluída e aprovada, Idempotency-Key e ausência de corpo.
PontuacaoController#reconciliarPostagem|Pontuação|Reconciliar pontos do descarte|200|Operação administrativa para ADMIN ou SERVICE. Ajusta estorno ou restauração conforme o estado da postagem. Exige Idempotency-Key e não aceita corpo.
PontuacaoController#consultarSaldo|Pontuação|Consultar saldo do usuário|200|Consulta o saldo acumulado conforme autorização de acesso ao usuário informado.
PontuacaoController#listarMovimentacoes|Pontuação|Consultar movimentações do usuário|200|Lista créditos e ajustes com paginação, conforme autorização.
PostagemValidacaoController#finalizar|Validação|Encerrar janela de validação|200|Operação administrativa para ADMIN. Encerra janela vencida e atualiza pontuação e confiança conforme as regras.
PostagemValidacaoController#decidir|Validação|Decidir postagem em análise|200|Disponível a ADMIN ou síndico autorizado pelo vínculo com o condomínio. Aprova ou reprova e atualiza a pontuação.
RankingController#consultarMoradores|Rankings|Consultar ranking de moradores|200|Consulta o ciclo atual do condomínio autorizado. O limite é de 1 a 100 participantes.
RankingController#consultarTorres|Rankings|Consultar ranking de torres|200|Consulta as torres do condomínio autorizado no ciclo atual. O limite é de 1 a 100 participantes.
RankingController#posicaoMorador|Rankings|Consultar posição do morador|200|Retorna posição e pontos no ranking. Participante sem posição pode ter posição nula.
RankingController#posicaoTorre|Rankings|Consultar posição da torre|200|Consulta posição e pontos de uma torre pertencente ao condomínio autorizado.
ReconstrucaoRankingController#reconstruir|Administração|Reconstruir rankings|200|Restrito a ADMIN. Reconstrói moradores, torres ou ambos a partir do PostgreSQL, somente para o ciclo ATUAL.
RedisAdminController#sincronizarPendentes|Administração|Sincronizar movimentações pendentes|200|Disponível a ADMIN ou SERVICE. Processa um lote de movimentações ainda não sincronizadas com Redis.
RedisAdminController#consultarStatus|Administração|Consultar diagnóstico do Redis|200|Restrito a ADMIN. Retorna disponibilidade e quantidade de movimentações pendentes.
TriagemController#triar|Validador|Executar triagem da foto|200|Disponível a ADMIN ou SERVICE. Analisa a foto da postagem existente. Pode rejeitar URL, download ou excesso de requisições simultâneas.
VotosController#votar|Validador|Registrar voto na validação|200|Payload em snake_case. usuario_id deve coincidir com o JWT. Denúncia exige motivo_denuncia_id.
DecisaoController#decidir|Validador|Registrar decisão do síndico|204|Payload em snake_case. usuario_id deve coincidir com o JWT e ser o síndico responsável pelo condomínio da postagem.
""";

        catalogo.lines()
                .filter(linha -> !linha.isBlank())
                .forEach(linha -> {
                    String[] partes = linha.split("\\|", 5);
                    mapa.put(
                            partes[0],
                            new Documento(
                                    partes[1],
                                    partes[2],
                                    partes[3],
                                    partes[4]));
                });

        return Map.copyOf(mapa);
    }

    private record Documento(
            String tag,
            String resumo,
            String status,
            String descricao) {
    }
}