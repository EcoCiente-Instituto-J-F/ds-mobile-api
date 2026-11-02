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
