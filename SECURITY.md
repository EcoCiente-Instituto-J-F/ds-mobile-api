# Política de Segurança

A `ds-mobile-api` atende o app mobile do EcoCiente e trata dados pessoais de moradores e síndicos (nome, e-mail, CPF, data de nascimento, endereço, telefones, vínculo com condomínios), além de postagens de descarte com URL e hash de fotos. Levamos a proteção desses dados a sério.

## Versões suportadas

| Versão | Suporte |
| --- | --- |
| `main` (mais recente) | Sim |
| Demais branches e tags antigas | Não |

## Reportando uma vulnerabilidade

**Não abra issue, PR ou discussão pública** para falhas de segurança.

Use o relato privado do GitHub: [abrir um advisory](https://github.com/EcoCiente-Instituto-J-F/ds-mobile-api/security/advisories/new) (aba **Security → Advisories → Report a vulnerability**).

> Mantenedores: ative o *Private vulnerability reporting* em **Settings → Code security** para que esse formulário fique disponível.

Inclua no relato:

- o impacto (quais dados ou ações ficam expostos);
- passos para reproduzir, usando **dados fictícios**;
- o commit testado e a configuração usada;
- se tiver, uma ideia de correção.

O que acontece depois:

1. Confirmamos o recebimento do relato.
2. Investigamos e desenvolvemos a correção em privado.
3. Publicamos o advisory com a correção, dando crédito a quem reportou (se desejar).

Pedimos um prazo razoável para corrigir antes de qualquer divulgação pública.

## Escopo

**Dentro do escopo:**

- acesso a dados de outro usuário ou condomínio;
- burla de autenticação ou autorização (JWT, perfis, vínculo aprovado com o condomínio);
- injeção (SQL, comandos) com exposição ou alteração de dados;
- vazamento de dados pessoais ou segredos em logs ou respostas;
- segredos commitados no repositório;
- abuso da integração com a `ds-pontuacao-api` (por exemplo, forjar pontuação).

**Fora do escopo:**

- bugs sem impacto de segurança;
- falhas em serviços de terceiros ou em outros repositórios do EcoCiente (reporte no repositório correspondente);
- credenciais de exemplo em arquivos de desenvolvimento;
- negação de serviço contra a infraestrutura de deploy.

## Como o sistema protege os dados

| Camada | Onde | O que faz |
| --- | --- | --- |
| Autenticação | `config/security/JwtAuthenticationFilter.java`, `JwtService.java` | Valida a assinatura HMAC do JWT, exige `exp` futuro, `usuarioId` positivo e `sub`; token inválido recebe 401 |
| Autorização por rota | `config/security/SecurityConfig.java` | Sessão stateless; toda rota exige autenticação, exceto Swagger e `/api-docs` |
| Identidade do usuário | controllers (`@AuthenticationPrincipal`) | O `usuarioId` vem do token, não da requisição |
| Regra de negócio | `core/service/CriarPostagemService.java` | Exige vínculo aprovado do usuário com o condomínio, valida categoria, URL, hash único e data de captura |
| Integração | `dataprovider/gateway/PontuacaoQuizGatewayImpl.java` | Chama a pontuação com token de serviço vindo de variável de ambiente, timeouts curtos e `Idempotency-Key` |
| Segredos | `application.properties`, `.gitignore` | Segredos lidos de variáveis de ambiente ou `.env`, que não é versionado |
| Container | `Dockerfile` | A aplicação roda com usuário sem privilégios |

## Checklist de deploy

- [ ] `JWT_SECRET` forte e único por ambiente, o mesmo do serviço que emite os tokens.
- [ ] Segredos (`JWT_SECRET`, `DB_PASSWORD`, `PONTUACAO_TOKEN_SERVICO`) fora de ConfigMap, imagem e repositório.
- [ ] Usuário de banco com o mínimo de permissões necessárias.
- [ ] Trocar toda senha ou token de exemplo usado em desenvolvimento.
- [ ] Avaliar se o Swagger UI e o `/api-docs`, públicos por padrão, devem ficar expostos no ambiente.
- [ ] Servir a API apenas por HTTPS.
