# Chat entre síndico e cooperativa

Usa as tabelas existentes `tb_conversas` e `tb_mensagens_conversas` informadas pelo responsável pelo banco. Não há migração de produção nesta alteração. O arquivo `chat-schema-test.sql` serve apenas ao banco isolado do Testcontainers.

Todas as rotas exigem `Authorization: Bearer TOKEN`.
A identidade vem do JWT; o lado do remetente é calculado pelo vínculo atual em `tb_condominios.sindico_id`/`tb_sindicos.usuario_id` ou `tb_cooperativas.usuario_id`. Não se aceita um remetente escolhido no corpo.
Usuário inativo, condomínio inativo ou cooperativa sem responsável ativo não podem operar a conversa. Um usuário representando as duas partes é recusado para evitar identidade ambígua.

| Método | Rota | Corpo/parâmetros |
| --- | --- | --- |
| POST | `/api/conversas` | `{"condominioId":1,"cooperativaId":1}` |
| GET | `/api/conversas` | `?arquivadas=false&page=0&size=20` |
| POST | `/api/conversas/{id}/mensagens` | `{"conteudo":"Olá! Podemos combinar uma coleta?"}` |
| GET | `/api/conversas/{id}/mensagens` | `?page=0&size=20` |
| PATCH | `/api/conversas/{id}/leitura` | `{"ateMensagemId":123}` ou sem corpo |
| PATCH | `/api/conversas/{id}/arquivamento` | `{"arquivada":true}`; `false` desarquiva |

## Contrato

- POST de conversa retorna 200 com a conversa criada ou existente. A constraint do banco garante uma conversa por par, inclusive com chamadas concorrentes. Repetir o POST não desarquiva.
- Qualquer uma das partes pode iniciar a conversa; não se exige coleta anterior. Somente representantes atuais têm acesso. A troca de síndico/responsável transfere o acesso ao histórico institucional.
- Conversa: `id`, `condominioId`, `cooperativaId`, `nomeCondominio`, `nomeCooperativa`, `criadaEm`, `ultimaMensagemEm`, `ativa`, `naoLidas`.
- Mensagem: `id` (BIGINT/Java Long), `conversaId`, `remetenteUsuarioId`, `remetenteTipo` (CONDOMINIO ou COOPERATIVA), `conteudo`, `enviadaEm`, `lidaEm`.
- POST de mensagem retorna 201. Texto normalizado nas bordas, de 1 a 2000 caracteres, persistido como texto simples; o cliente deve renderizar sem interpretar HTML.
- Páginas: `total`, `pagina`, `tamanho`, `totalPaginas`, `itens`. Página a partir de zero, tamanho de 1 a 100. Página além do final preserva o total e retorna `itens: []`.
- Conversas por última atividade decrescente, com desempate por ID. Mensagens mais recentes primeiro, com desempate por ID; a página zero contém as últimas mensagens.
- Listar mensagens não marca leitura automaticamente. PATCH de leitura marca somente recebidas ainda não lidas, preserva datas anteriores e retorna `{"mensagensMarcadas":N}`. Com limite, marca apenas IDs menores ou iguais ao limite dentro da conversa; sem corpo, marca todas as recebidas existentes. O app deve enviar o maior ID que efetivamente exibiu.
- Envio, leitura e arquivamento são serializados por lock na conversa. A mensagem e `ultima_mensagem_em` são gravadas na mesma transação. Falhas revertem ambas as alterações.

## Arquivamento disponível neste schema

`tb_conversas.ativa` é global. Arquivar afeta ambos os participantes, preserva o histórico e permite leitura, mas bloqueia envio com 409. A consulta padrão mostra ativas; `arquivadas=true` mostra arquivadas. PATCH retorna 204 e pode ser repetido.

Arquivamento individual exige mudança futura no modelo do banco. Esta implementação não promete arquivamento individual, WebSocket nem push: o cliente consulta as rotas REST.

## Erros

400 para parâmetros/corpo inválidos; 401 para JWT ausente/inválido; 403 para usuário sem permissão; 404 para conversa inexistente; 409 para envio em conversa arquivada. A criação com partes inexistentes/inativas recebe 403, pois não há autorização para o par informado.

## Testes

```powershell
.\gradlew.bat test --tests "*ChatServiceTest"
.\gradlew.bat test --tests "*ChatIntegrationTest"
.\gradlew.bat clean build --no-daemon
```

Java 21 e Docker necessários para integração. Testes cobrem criação concorrente, remetente, isolamento, leitura com limite, repetição de leitura, arquivamento compartilhado, preservação do histórico, paginação e limites de conteúdo.
