# Notificações reais — implantação controlada

## Escopo desta PR
Primeiro incremento: persistência de dispositivos por instalação, endpoints JWT, histórico, processador FCM, rotinas de engajamento, e eventos de quiz, EcoRegistro, ranking e coletas. Não está ativado em produção. Outros tipos ainda dependem de integrações adicionais (mensagens, avisos do síndico, alteração de calendário e novo destinatário de solicitação).

## Dependências e precauções
- Aplicar o script `scripts/sql/20261030_push_notificacoes.sql` **no banco correto**, com backup e revisão DBA, antes de iniciar workers.
- Verificar `FIREBASE_CREDENCIAIS` no servidor; não versionar a chave de serviço.
- `app.push.enabled` permanece **false** por padrão. Ativar com variável de ambiente `APP_PUSH_ENABLED=true` ou propriedade equivalente **após** migração, credenciais, CI, publicação e testes.
- `app.push.dispatch-delay-ms` controla intervalo de consumo de entrega, padrão 15000 ms.
- A tabela `tb_notificacoes` anterior é preservada; apenas linhas com `chave_evento` são tratadas como notificações reais novas.
- Não configurar dois workers independentes sem revisar locking, custos e modo de contingência AWS/Render.

## Contrato inicial
`POST /api/notificacoes/dispositivos` autenticado, body JSON com `instalacaoId` UUID, `token` FCM e `preferencias`. O `usuarioId` é o JWT validado, nunca o corpo da requisição.
`DELETE /api/notificacoes/dispositivos/{instalacaoId}` desativa o próprio dispositivo.
`GET /api/notificacoes?limite=50` retorna histórico da conta.
`PATCH /api/notificacoes/{notificacaoId}/lida` marca uma notificação da conta como lida.

## Limitações atuais e próximos incrementos
- Mensagem nova: precisa identificar o usuário representante da parte destinatária.
- Aviso de condomínio: a UI atual salva rascunho local; precisa publicação real, permissões e audiência.
- Coleta alterada: endpoint atual altera status, não agenda; mudanças de data/hora necessitam evento específico.
- Solicitação nova: exige consulta segura de representantes da cooperativa/condomínio destinatário.
- Notificações de retorno usam `ultimo_acesso_em` do cadastro de dispositivos como proxy de atividade, até existir atividade de sessão no servidor.
- Os jobs de rotina têm frequência conservadora e devem ser validados em homologação com usuários voluntários.
- Testes de entrega em segundo plano, app encerrado, dois aparelhos e Android 13+ são obrigatórios.

## Gate de audiência para o piloto FCM (release 1.1.6)

Os jobs de push continuam desativados por padrão com `APP_PUSH_ENABLED=false`.
Mesmo quando um único worker for explicitamente ativado, `APP_PUSH_AUDIENCE=disabled` (padrão) impede que o backend crie ou entregue novos pushes. A audiência deve ser selecionada conscientemente:

- `APP_PUSH_AUDIENCE=pilot` e `APP_PUSH_PILOT_USER_IDS=63`: somente o usuário 63 pode receber pushes gerados pelo backend. IDs adicionais podem ser separados por vírgula e precisam ser positivos.
- `APP_PUSH_AUDIENCE=all`: todos os usuários elegíveis podem receber notificações. **Não usar no primeiro teste.**
- `APP_PUSH_AUDIENCE=disabled`: não cria nem despacha notificações pela API.

No piloto, **habilitar `APP_PUSH_ENABLED=true` em apenas uma instância**: preferencialmente o worker AWS depois da publicação e validação da imagem 1.1.6. Manter `APP_PUSH_ENABLED=false` no Render e em quaisquer outras réplicas. Não ativar por engano dois workers apontando para o mesmo Neon.

O `PushDispatchJob` verifica a audiência novamente antes de cada envio e descarta entregas fora do público autorizado, mesmo se já estiverem na fila. O `JdbcPushService` restringe a geração de notificações e a seleção de destinatários automáticos. Dispositivos de outros usuários continuam podendo se registrar na API sem receber mensagens do piloto.

Antes de liberar para `all`, testar entrega FCM **data-only** em primeiro e segundo plano, conta com dois aparelhos, preferências desligadas, logout e registros nas tabelas `tb_notificacoes` e `tb_push_entregas`. Verificar as credenciais de Firebase no worker autorizado sem exibi-las em logs.
