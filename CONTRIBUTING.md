# Contribuindo com a ds-mobile-api

Ao participar, você concorda em seguir o [Código de Conduta](CODE_OF_CONDUCT.md). Para reportar um problema de segurança, **não** abra uma issue — siga o [SECURITY.md](SECURITY.md).

## Idioma

Código de domínio, mensagens de erro, commits, issues e PRs são escritos em português do Brasil, como no restante do projeto.

## Configurando o ambiente

1. Instale o JDK 21 e o Docker.
2. Faça o fork ou clone o repositório:
   ```bash
   git clone https://github.com/EcoCiente-Instituto-J-F/ds-mobile-api.git
   cd ds-mobile-api
   ```
3. Copie `.env.example` para `.env` e preencha `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e `JWT_SECRET` (veja as variáveis no [README](README.md)). O `.env` está no `.gitignore`; nunca o commite.
4. Rode a aplicação com `./gradlew bootRun` (ou `gradlew.bat bootRun` no Windows).

## Testando sua mudança

O CI executa:

```bash
./gradlew clean build --no-daemon
```

Rode o mesmo comando antes de abrir a PR. Os testes de integração usam Testcontainers, então o Docker precisa estar ativo.

- Toda mudança de comportamento vem com um teste que falha sem ela.
- Os testes não usam rede, APIs pagas nem dados de produção. A integração com a `ds-pontuacao-api` não é chamada nos testes.
- Dados de teste ficam em `src/test/resources/sql`.

## Regras do projeto

- **Arquitetura em camadas:** `entrypoint` (controllers, DTOs) → `core` (services, domínio, interfaces de gateway) → `dataprovider` (implementações, entidades, repositórios). O `core` não depende de `entrypoint` nem de `dataprovider`.
- **Identidade vem do token:** o `usuarioId` é lido do JWT (`@AuthenticationPrincipal`), nunca de parâmetro enviado pelo cliente.
- **Rotas protegidas por padrão:** qualquer exceção à autenticação em `SecurityConfig` precisa ser discutida na PR.
- **Banco:** o schema é gerenciado fora deste repositório (`ddl-auto=validate`); não crie tabelas por aqui.
- **Configuração em `application.properties`:** variáveis novas entram lá, com a documentação correspondente no README e no `.env.example`.
- Nenhum segredo, token ou senha no repositório.
- Nenhum dado pessoal real em testes ou exemplos.
- Nenhuma dependência nova no `build.gradle` sem discussão prévia.
- A versão em `build.gradle` (`MAJOR.MINOR.PATCH`) define a tag da imagem Docker publicada na `main`, e tags são imutáveis: mude a versão apenas quando for publicar.

## Pull requests

1. Crie a branch a partir da `main` (por exemplo `feat/nome-curto` ou `fix/nome-curto`).
2. Faça commits focados, com prefixos `feat:`, `fix:`, `chore:`, `docs:`, `test:` ou `refactor:`.
3. Abra a PR para a `main`. A descrição é gerada pelo PR bot; revise-a e complete os checklists.
4. O CI precisa passar.
5. Um mantenedor revisa e aprova antes do merge.

## Reportando bugs e propondo funcionalidades

Abra uma issue descrevendo o comportamento esperado e o observado, com passos para reproduzir (usando dados fictícios) e a versão ou commit afetado. Para novas funcionalidades, explique o problema que ela resolve antes de propor a implementação.
