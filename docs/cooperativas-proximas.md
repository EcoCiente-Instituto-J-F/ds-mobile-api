# Cooperativas da região do síndico

## Endpoint

`GET /api/cooperativas?page=0&size=100`

A rota exige JWT. O identificador do usuário autenticado é usado para localizar o síndico e os condomínios ativos vinculados a ele.

## Resposta

Além dos metadados de paginação e da lista `cooperativas`, a resposta inclui:

- `condominioId`: ID do único condomínio ativo associado ao síndico autenticado.
- `total`, `pagina`, `tamanho`, `totalPaginas`.
- `cooperativas[]`: os dados cadastrais, endereço e avaliações já retornados pela API.

Exemplo:

```json
{
  "total": 12,
  "pagina": 0,
  "tamanho": 100,
  "totalPaginas": 1,
  "condominioId": 6,
  "cooperativas": [
    {
      "id": 62,
      "nome": "Cooperativa EcoCiente",
      "email": "contato@example.com",
      "telefone": "11999990000",
      "enderecoCompleto": "Rua Exemplo, 10, São Paulo - SP",
      "cidade": "São Paulo",
      "estado": "SP",
      "mediaAvaliacoes": 4.5,
      "totalAvaliacoes": 8
    }
  ]
}
```

O exemplo acima é ilustrativo; os valores reais vêm do banco.

## Regra de resolução do condomínio

A resolução usa o relacionamento existente:

`tb_usuarios.id_usuario → tb_sindicos.usuario_id → tb_sindicos.id_sindico → tb_condominios.sindico_id → tb_condominios.id_condominio`.

Não há ID de condomínio fixo no aplicativo. Se a conta possuir zero ou mais de um condomínio ativo, `condominioId` será `null`; o cliente deve bloquear a criação de pedido em vez de escolher um ID arbitrariamente.

Para o registro validado durante a implementação, o usuário 61 está associado ao síndico 6 e ao condomínio ativo 6 (Residencial EcoCiente).

## Região e distância

A consulta de cooperativas continua usando correspondência de cidade e estado entre endereço do condomínio e endereço da cooperativa. O banco/API não fornece latitude/longitude nessa resposta; portanto, essa rota não calcula distância em quilômetros.

## Solicitação

O cliente usa o endpoint já existente:

`POST /api/v1/agendamentos`

Com `condominioId`, `cooperativaId` e `dataInicio` (OffsetDateTime futuro). O registro nasce com status `AGENDADO`, aguardando a cooperativa destinatária aceitar (`CONFIRMADO`) ou recusar (`RECUSADO`) pelo fluxo de status existente.
