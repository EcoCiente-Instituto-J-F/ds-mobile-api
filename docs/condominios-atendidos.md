# Condomínios atendidos pela cooperativa

`GET /api/cooperativas/{id}/condominios?page=0&size=10`

Exige JWT do responsável ativo pela cooperativa. Outro usuário recebe 403;
cooperativa inexistente ou sem responsável ativo recebe 404.
Página inicia em zero; tamanho permitido de 1 a 100. Parâmetros inválidos retornam 400.
Sem resultados ou além da última página, retorna 200 com lista vazia e total preservado.

A resposta contém `cooperativaId`, `total`, `pagina`, `tamanho`, `totalPaginas`
e `condominios`. Cada condomínio contém `id`, `nome`, `endereco`,
`totalTorres` e `diasColeta`. Endereço pode ser nulo.

## Critério adotado

O schema versionado não contém vínculo de parceria entre cooperativa e condomínio.
Esta consulta considera atendido o condomínio ativo com pelo menos uma coleta
CONFIRMADA ou REALIZADA da cooperativa. Solicitações AGENDADAS, RECUSADAS ou
CANCELADAS não estabelecem atendimento. Coletas múltiplas não duplicam o condomínio.
Uma coleta realizada mantém o condomínio no histórico de atendimento.

`diasColeta` reúne nomes distintos de dias das recorrências de coletas CONFIRMADAS
com `possui_recorrencia=true`, em ordem alfabética. Coletas realizadas não definem
uma agenda recorrente atual. Não se infere recorrência de uma coleta avulsa.
A ordenação dos condomínios é por nome e ID, com paginação no banco.

**Unidades/apartamentos:** o schema versionado não possui a estrutura; a resposta
não inventa contagens nem usa o número de moradores como número de unidades.
`totalTorres` é somente a quantidade de registros em `tb_torres`.

## Validação

```powershell
.\gradlew.bat test --tests "*ConsultarCondominiosAtendidosServiceTest"
.\gradlew.bat test --tests "*CondominiosAtendidosIntegrationTest"
.\gradlew.bat clean build --no-daemon
```

A integração exige Docker e usa PostgreSQL isolado via Testcontainers.
