package ofc.bot.handlers.interactions.commands.responses.states;

import ofc.bot.util.Bot;

public enum Status implements InteractionResult {
    // These are separate constants to responses where only
    // embeds/images/videos are sent.
    OK(        State.OK,         true),
    PROCESSING(State.PROCESSING, false, "> <a:loading:1293036166387601469> Processando..."),

    /* -------------------- PASSED -------------------- */
    DONE(                                       State.OK, true, "Pronto! 😎"),
    BALANCE_SET_SUCCESSFULLY(                   State.OK, true, "Saldo de %s definido para `$%s`."),
    TRANSACTION_SUCCESSFUL(                     State.OK, true, "Você transferiu `$%s` para %s!"),
    SUCCESSFULLY_DISCONNECTING_USERS(           State.OK, true, "Desconectando `%s` usuários de `%s`."),
    SUCCESSFULLY_MOVING_USERS(                  State.OK, true, "Movendo `%s` usuários para `%s`."),
    LOADING_CHANNEL_MESSAGES(                   State.OK, true, "> <a:loading:1293036166387601469> Baixando mensagens...\n> Você será avisado no privado ao concluir.\n> Mensagem mais antiga: `%s`.\n> Total atual: `%s`."),
    BIRTHDAY_ADDED_SUCCESSFULLY(                State.OK, true, "Aniversário de %s (`%s`) salvo com sucesso!"),
    BIRTHDAY_DELETED_SUCCESSFULLY(              State.OK, true, "Aniversário de <@%d> foi removido com sucesso."),
    DIVORCED_SUCCESSFULLY(                      State.OK, true, "É, parece que as coisas não deram certo por aqui e vocês tiveram que se separar. 😕"),
    CONFIRMATION_CODE_ALREADY_SENT(             State.OK, true, "Um código de confirmação já foi enviado em seu privado! Por favor, utilize-o para acessar este recurso."),
    MARRIAGE_PROPOSAL_SENT_SUCCESSFULLY(        State.OK, true, "Proposta enviada com sucesso."),
    SUBSCRIPTION_SUCCESSFULLY_SAVED(            State.OK, true, "Inscrição salva com sucesso!"),
    SUBSCRIPTION_SUCCESSFULLY_DELETED(          State.OK, true, "Inscrição removida com sucesso!"),
    ECONOMY_SUCCESSFULLY_UPDATED_BALANCE(       State.OK, true, "Saldo total de %s atualizado para: `$%s`."),
    DAILY_SUCCESSFULLY_COLLECT(                 State.OK, true, "> ✨ Você ganhou `$%s` em daily hoje!"),
    DAILY_SUCCESSFULLY_COLLECTED_BOOSTING(      State.OK, true, "> 💎 Hoje você ganhou `$%s` em daily, incluindo um adicional de `$%s` por ser Nitro Booster!"),
    WORK_SUCCESSFUL(                            State.OK, true, "> 💼 Parabéns pelo seu trabalho rs, você ganhou `$%s`!"),
    WORK_SUCCESSFUL_BOOSTING(                   State.OK, true, "> 👑 Eita 🥵 parabéns pelo seu trabalho, você ganhou `$%s` hoje, com um acréscimo de `$%s` por ser booster."),
    GROUP_BOT_SUCCESSFULLY_ADDED(               State.OK, true, "Bot %s adicionado com sucesso ao seu grupo."),
    GROUP_PERMISSION_GRANTED_SUCESSFULLY(       State.OK, true, "Permissão `%s` concedida com sucesso!"),
    PROPOSAL_REMOVED_SUCCESSFULLY(              State.OK, true, "Proposta de casamento enviada à %s foi removida com sucesso."),
    ROLE_SUCCESSFULLY_ADDED_TO_MEMBER(          State.OK, true, "O cargo `%s` foi adicionado com sucesso à %s."),
    POLICIES_CACHE_SUCCESSFULLY_INVALIDATED(    State.OK, true, "O cache das políticas de módulos foi invalidado com sucesso!"),
    MARRIAGE_PROPOSAL_REJECTED_SUCCESSFULLY(    State.OK, true, "É, parece que não foi dessa vez 😔"),
    NO_LEVEL_ROLE_FOUND(                        State.OK, true, "Nenhum cargo de nível encontrado."),
    MESSAGE_SUCCESSFULLY_PINNED(                State.OK, true, "Mensagem fixada com sucesso."),
    POLICY_SUCCESSFULLY_DELETED(                State.OK, true, "Regra apagada com sucesso."),
    MESSAGE_SUCCESSFULLY_UNPINNED(              State.OK, true, "Mensagem desfixada com sucesso."),
    NO_PENDING_INVOICES(                        State.OK, true, "Não há nenhuma fatura pendente."),
    GROUP_OWNERSHIP_TRANSFERRED_SUCCESSFULLY(   State.OK, true, "A posse do grupo foi transferida com sucesso para %s!"),
    MARRIAGE_PROPOSAL_ACCEPTED_SUCCESSFULLY(    State.OK, true, "Pedido de casamento aceito com sucesso. \uD83D\uDE03"),
    USER_INVITING_TO_TICTACTOE(                 State.OK, true, "%s Você foi convidado a jogar Jogo da velha!"),
    USERINFO_RESET_SUCCESSFULLY(                State.OK, true, "Customizações do userinfo resetadas."),
    CONFIRMATION_CODE_SENT_SUCCESSFULLY_TIP(    State.OK, true, "Um código de confirmação foi enviado no seu privado! Utilize este código %s."),
    USERINFO_COLOR_SUCCESSFULLY_UPDATED(        State.OK, true, "Cor do userinfo atualizada com sucesso."),
    NO_COLOR_ROLES_STATE(                       State.OK, true, "Nenhum cargo de cor presente."),
    CREATING_GROUP(                             State.OK, true, "<a:loading:1293036166387601469> criando grupo..."),
    MESSAGE_ALREADY_PINNED(                     State.OK, true, "Esta mensagem já está fixada."),
    MESSAGE_ALREADY_UNPINNED(                   State.OK, true, "Esta mensagem já não está fixada."),
    USERINFO_DESCRIPTION_SUCCESSFULLY_UPDATED(  State.OK, true, "Descrição do userinfo atualizada com sucesso."),
    POLICY_SUCCESSFULLY_CREATED(                State.OK, true, "Regra salva/criada com sucesso!"),
    USERINFO_FOOTER_SUCCESSFULLY_UPDATED(       State.OK, true, "Rodapé do userinfo ataulizado com sucesso."),
    ROLES_SUCCESSFULLY_BACKED_UP(               State.OK, true, "Foram devolvidos `%d` cargos com sucesso para `%s`."),
    POLL_CLOSED(                                State.OK, true, "Enquete `%s` foi fechada com sucesso."),
    CHANNELS_STATE_TOGGLED_SUCCESSFULLY(        State.OK, true, "Canais %s com sucesso!"),
    POLL_REOPENED(                              State.OK, true, "Enquete `%s` foi reaberta com sucesso."),
    MEMBER_SUCCESSFULLY_BANNED(                 State.OK, true, "Membro %s banido com sucesso!"),
    MEMBER_SUCCESSFULLY_UNBANNED(               State.OK, true, "Membro %s desbanido com sucesso!"),
    ALL_STAFF_LIST_MESSAGES_UPDATED(            State.OK, true, "Todas as mensagens encontradas foram editadas."),
    MESSAGES_SUCCESSFULLY_DELETED(              State.OK, true, "`%02d` mensagens foram apagadas com sucesso em `%s`."),
    GROUP_SUCCESSFULLY_DELETED(                 State.OK, true, "Seu grupo `%s` foi apagado com sucesso."),
    USER_DOES_NOT_HAVE_RANK(                    State.OK, true, "Este usuário ainda não tem um rank."),
    TICKET_CLOSED_SUCCESSFULLY(                 State.OK, true, "> Ticket `#%02d` fechado com sucesso!"),
    TICKET_OPENED_SUCCESSFULLY(                 State.OK, true, "> Ticket aberto com sucesso em %s!"),
    GROUP_SUCCESSFULLY_UPDATED(                 State.OK, true, "Os dados fornecidos foram aplicados com sucesso ao seu grupo."),
    GROUP_SUCCESSFULLY_CREATED(                 State.OK, true, "✅ Grupo %s criado com sucesso! Você pode usar o comando `/group member add` até 4 (quatro) vezes de graça."),
    GROUP_CHANNEL_SUCCESSFULLY_CREATED(         State.OK, true, "O canal para seu grupo foi criado com sucesso em %s!"),
    GROUP_CHANNEL_SUCCESSFULLY_DELETED(         State.OK, true, "Canal de grupo de tipo `%s` foi deletado com sucesso."),
    MEMBER_SUCCESSFULLY_ADDED_TO_GROUP(         State.OK, true, "O membro %s foi adicionado com sucesso no seu grupo."),
    MEMBER_SUCCESSFULLY_REMOVED_FROM_GROUP(     State.OK, true, "O membro <@%d> foi removido com sucesso do seu grupo."),
    NOTHING_CHANGED_WITH_REASON(                State.OK, true, "Nada foi alterado, pois %s."),
    LEADERBOARD_IS_EMPTY(                       State.OK, true, "O placar de líderes está vazio."),
    PAGE_IS_EMPTY(                              State.OK, true, "A página está vazia."),
    NO_TICKETS_FOUND(                           State.OK, true, "Nenhum ticket encontrado."),
    MARRIAGE_LIST_IS_EMPTY(                     State.OK, true, "Nenhum casamento encontrado."),
    CONFIRM_GROUP_CHANNEL_CREATION(             State.OK, true, "> Tem certeza que deseja comprar este canal? Tipo: `%s`"),
    GROUP_PASSWORD_UPDATED_SUCCESSFULLY(        State.OK, true, "> Senha do grupo atualizada com sucesso!"),
    CONFIRM_GROUP_UPDATE(                       State.OK, true, "Você está prestes a alterar alguns atributos de nome e/ou cor seu grupo. Deseja confirmar?"),
    CONFIRM_GROUP_MEMBER_ADD(                   State.OK, true, "Você está prestes a adicionar %s ao seu grupo. Por favor, verifique o valor abaixo e confirme o pagamento."),
    CONFIRM_GROUP_MEMBER_REMOVE(                State.OK, true, "Você está prestes a remover %s de seu grupo. Por favor, confirme sua decisão."),
    MARRIAGE_PROPOSAL_LIST_IS_EMPTY(            State.OK, true, "Nenhuma proposta encontrada para os argumentos fornecidos."),
    USER_HAS_NO_INFRACTIONS(                    State.OK, true, "O usuário não tem nenhuma infração."),
    YOU_DONT_HAVE_REMINDERS(                    State.OK, true, "Você não possui nenhum lembrete."),
    INFRACTION_SUCCESSFULLY_DELETED(            State.OK, true, "Infração deletada com sucesso!"),
    SUCCESSFULLY_REMOVED_FROM_GROUP(            State.OK, true, "Você foi removido(a) do grupo `%s` com sucesso!"),
    ROLE_HAS_NO_MEMBERS(                        State.OK, true, "Não tem nenhum membro no cargo fornecido"),
    GUILD_HAS_NO_ICON(                          State.OK, true, "O servidor atual não tem nenhum ícone."),
    INVOICE_SUCCESSFULLY_PAID(                  State.OK, true, "Fatura de `%s` paga com sucesso!"),
    NO_MESSAGES_FOUND(                          State.OK, true, "> Nenhuma mensagem encontrada."),
    MEMBER_ALREADY_HAS_ROLE(                    State.OK, true, "O membro já tem o cargo `%s`."),
    NO_GIF_WAS_FOUND(                           State.OK, true, "Nenhum GIF encontrado."),
    MEMBER_SUCCESSFULLY_TIMED_OUT_FOR(          State.OK, true, "Membro %s silenciado com sucesso por `%s`!"),
    TIMEOUT_REMOVED_SUCCESSFULLY(               State.OK, true, "Timeout removido com sucesso de %s!"),
    TICKET_MERGED_SUCCESSFULLY(                 State.OK, true, "Tickets mesclados com sucesso!"),
    NO_GUILD_AVATAR_PRESENT(                    State.OK, true, "O membro não possui um avatar específico para este servidor."),
    USER_DID_NOT_VOTE_IN_POLL(                  State.OK, true, "O usuário não votou nessa enquete."),
    FAILED_TO_ROB_USER(                         State.OK, true),
    ROBBED_USER_SUCCESSFULLY(                   State.OK, true),


    /* -------------------- ERROR -------------------- */
    WAIT_BEFORE_WORK_AGAIN(                     State.FAILURE, true),
    PLEASE_WAIT_COOLDOWN(                       State.FAILURE, false,  "Por favor, aguarde `%s`."),
    REQUEST_REJECTED(                           State.FAILURE, true,  "Pedido negado."),
    ERRORS_ENCOUNTERED(                         State.FAILURE, true,  "Não foi possível completar a operação! Erros:\n\n%s"),
    NOT_IMPLEMENTED(                            State.FAILURE, true,  "Comando não implementado."),
    CANNOT_DIVORCE_YOURSELF(                    State.FAILURE, true,  "Você não pode divorciar-se de você mesmo."),
    USER_IS_NOT_MARRIED_TO_TARGET(              State.FAILURE, true,  "Você não está casado(a) com %s"),
    INFRACTION_NOT_FOUND(                       State.FAILURE, true,  "Infração não encontrada."),
    YOU_GOT_NO_MONEY_TO_WITHDRAW(               State.FAILURE, true),
    YOU_GOT_NO_MONEY_TO_DEPOSIT(                State.FAILURE, true),
    YOU_CANNOT_DO_THIS_WHILE_BETTING(           State.FAILURE, false, "Você não pode fazer isso enquanto tiver uma aposta ativa."),
    MEMBER_IS_BETTING_PLEASE_WAIT(              State.FAILURE, false, "O membro <@%d> já está em uma aposta! Por favor, aguarde."),
    MEMBER_NOT_FOUND(                           State.FAILURE, true,  "Membro não encontrado."),
    MAX_CHOICES_GREATER_THAN_TOTAL_OPTIONS(     State.FAILURE, true,  "A quantidade máxima de opções selecionáveis (`%d`) é maior que o total de opções existentes (`%d`)."),
    SAME_EXPRESSION_REMINDER_FOUND(             State.FAILURE, false, "Já existe outro lembrete com este mesmo padrão."),
    NO_OPTIONS_AT_SELECT_MENU(                  State.FAILURE, true,  "0 opções válidas foram encontradas para o menu de seleções."),
    TARGET_WALLET_IS_EMPTY(                     State.FAILURE, true,  "O usuário não tem nenhum dinheiro na carteira."),
    YOU_CANNOT_ROB_YOURSELF(                    State.FAILURE, true,  "Você não pode roubar você mesmo."),
    REMINDER_NOT_FOUND(                         State.FAILURE, true,  "Lembrete não encontrado."),
    INVALID_CRON_EXPRESSION(                    State.FAILURE, false, "A Cron Expression é inválida: `%s`."),
    EMPTY_BANK_STATEMENT(                       State.FAILURE, true,  "Seu extrato está vazio."),
    FAKE_PI_JOKE(                               State.FAILURE, true,  "Kkkkkkkkkkkkkkkkkkkkkkkkk sério mesmo que você " +
            "achou que eu iria sugerir o valor certo de PI para encurtar seu caminho? " +
            "Sendo que a ideia de pedir essa confirmação é literalmente para dificultar a sua decisão " +
            "e evitar que você cometa erros. Aiai 😘"),
    CANNOT_LEAVE_YOUR_OWN_GROUP(                State.FAILURE, true,  "Você não pode sair do seu próprio grupo."),
    USER_NOT_FOUND(                             State.FAILURE, true,  "Usuário não encontrado."),
    YOU_ARE_BANNED_FROM_THIS_BOT(               State.FAILURE, true,  "> ❌ Você está banido deste bot."),
    BOT_CANNOT_BAN_PROVIDED_MEMBER(             State.FAILURE, true,  "Eu não posso banir este usuário."),
    BOT_CANNOT_KICK_PROVIDED_MEMBER(            State.FAILURE, true,  "Eu não posso expulsar este usuário."),
    YOU_CANNOT_BET_THIS_USER(                   State.FAILURE, true,  "Você não pode apostar com você mesmo ou com outros bots."),
    POLICY_RULE_NOT_FOUND(                      State.FAILURE, true,  "Nenhuma regra encontrada."),
    CANNOT_MERGE_SAME_TICKET(                   State.FAILURE, false, "Você não pode mesclar um ticket nele mesmo."),
    TICKET_ALREADY_CLOSED(                      State.FAILURE, false, "> ❌ Ticket já foi fechado."),
    DESTINATION_CHANNEL_NOT_FOUND(              State.FAILURE, false, "> ❌ O canal do ticket de destino não foi encontrado."),
    BOT_CANNOT_TIMEOUT_PROVIDED_MEMBER(         State.FAILURE, true,  "Eu não posso silenciar este usuário."),
    YOU_CANNOT_RUN_THIS_COMMAND(                State.FAILURE, false, "> ❌ Você não pode executar este comando."),
    BOT_CANNOT_REMOVE_TIMEOUT_OF_MEMBER(        State.FAILURE, true,  "Eu não posso remover o timeout deste usuário."),
    MEMBER_IS_NOT_TIMED_OUT(                    State.FAILURE, true,  "Este membro não está silenciado."),
    GROUP_PERMISSION_ALREADY_GRANTED(           State.FAILURE, true,  "Esta permissão já está habilitada."),
    COMMAND_IS_ALREADY_OPERATING(               State.FAILURE, true,  "Este comando já está em execução! Por favor, aguarde!"),
    GROUP_NAMES_CANNOT_CONTAIN_EMOJIS(          State.FAILURE, true,  "Nomes de grupos não podem ter emojis."),
    INVALID_DURATION_PROVIDED(                  State.FAILURE, true,  "Duração inválida."),
    PERIOD_TOO_SHORT(                           State.FAILURE, false, "Período curto demais!"),
    INVALID_PERIOD_PROVIDED(                    State.FAILURE, false, "Período inválido."),
    UNSUPPORTED_RESOURCE_TYPE(                  State.FAILURE, true,  "A regra `%s` não suporta o tipo de entidade fornecida (`%s`)! Os tipos de entidades aceitas para esta regra são: `%s`."),
    INVALID_DELETION_TIMEFRAME(                 State.FAILURE, true,  "Período inválido para deleção de histórico! Máximo: `%s`, fornecido: `%s`."),
    INVALID_TIMEOUT_DURATION(                   State.FAILURE, true,  "Duração inválida para timeout! Máximo: `%s`, fornecido: `%s`."),
    PAGE_DOES_NOT_EXIST(                        State.FAILURE, true,  "A página fornecida não existe! Max: `%d`."),
    MEMBER_NOT_IN_GUILD(                        State.FAILURE, true,  "O membro fornecido não está no servidor."),
    INVALID_COLOR_PROVIDED(                     State.FAILURE, true,  "A cor fornecida é inválida."),
    COLOR_ROLE_NOT_FOUND(                       State.FAILURE, false, "A cor informada não foi encontrada."),
    WALLET_CANNOT_BE_NEGATIVE(                  State.FAILURE, true,  "A carteira (cash) não pode ficar negativada."),
    USER_IS_NOT_BANNED_FROM_GUILD(              State.FAILURE, true,  "O usuário %s não está banido deste dservidor."),
    INCORRECT_PASSWORD(                         State.FAILURE, true,  "A senha está incorreta."),
    TARGET_IS_IMMUNE_TO_PUNISHMENTS(            State.FAILURE, true,  "O usuário fornecido é imune à punições."),
    DAILY_ALREADY_COLLECTED(                    State.FAILURE, true,  "> ❌ Você já coletou seu daily hoje! Aguarde <t:946692000:t> para usar novamente."),
    YOU_DO_NOT_OWN_A_GROUP(                     State.FAILURE, true,  "Você não possui nenhum grupo."),
    YOU_CAN_NO_LONGER_PAY_THIS_INVOICE(         State.FAILURE, true,  "Você não pode mais pagar esta fatura."),
    CANNOT_TRANSFER_TO_YOURSELF(                State.FAILURE, true,  "Você não pode transferir dinheiro para você mesmo."),
    CANNOT_TRANSFER_TO_BOTS(                    State.FAILURE, true,  "Você não pode transferir dinheiro para outros bots."),
    STAFF_MESSAGES_UPDATE_STILL_IN_PROGRESS(    State.FAILURE, true,  "Algumas mensagens de `staff-oficina` ainda estão sendo atualizadas! Por favor, aguarde."),
    EMOJI_OPTION_CAN_ONLY_CONTAIN_EMOJI(        State.FAILURE, true,  "A opção `emoji` pode apenas conter um emoji! Se você forneceu um emoji válido mas ele não foi reconhecido pelo bot, entre em contado com <@596939790532739075>."),
    INVALID_CHANNEL_TYPE(                       State.FAILURE, true,  "O tipo de canal fornecido é inválido: `%s`."),
    HIT_MAX_PINNED_MESSAGES(                    State.FAILURE, true,  "O número máximo de mensagens fixadas no canal já foi atingido."),
    HIT_MAX_REMINDERS(                          State.FAILURE, true,  "O número máximo de lembretes ativos já foi atingido! Atualmente: `%d`."),
    FAILED_TO_DM_USER(                          State.FAILURE, true,  "Não foi possível enviar mensagem em seu privado! Por favor, abra-o para que você possa prosseguir com o utilizo deste comando/recurso."),
    USER_CANNOT_RECEIVE_GIVEN_AMOUNT(           State.FAILURE, true,  "Este usuário não pode receber esta quantia! Possivelmente ultrapassaria o limite de saldo (`" + Bot.fmtMoney(Integer.MAX_VALUE) + "`)."),
    YOU_ALREADY_HAVE_THIS_COLOR_ROLE(           State.FAILURE, false, "Você já tem este cargo de cor."),
    YOU_DO_NOT_HAVE_THIS_COLOR_ROLE(            State.FAILURE, false, "Você não tem este cargo de cor."),
    NO_NAME_HISTORY_FOR_USER(                   State.FAILURE, true,  "Sem histórico de nomes para `%s`."),
    YOU_ARE_NOT_IN_THE_PROVIDED_GROUP(          State.FAILURE, true,  "Você não está no grupo fornecido."),
    MEMBER_ALREADY_IN_THE_GROUP(                State.FAILURE, true,  "O membro fornecido já está no grupo."),
    CANNOT_ADD_BOTS_TO_GROUP(                   State.FAILURE, true,  "Não é possível adicionar bots em grupos (como um membro)."),
    MEMBER_NOT_IN_THE_GROUP(                    State.FAILURE, true,  "O membro fornecido não está no grupo."),
    SAME_CHANNEL_FOR_MULTIPLE_ARGUMENTS(        State.FAILURE, true,  "Você não pode fornecer o mesmo canal de voz em ambos os argumentos."),
    VOICE_CHANNEL_IS_EMPTY(                     State.FAILURE, true,  "O canal de voz `%s` está vazio."),
    NO_USERS_DISCONNECTED(                      State.FAILURE, true,  "Nenhum usuário foi desconectado."),
    NO_USERS_MOVED(                             State.FAILURE, true,  "Nenhum usuário foi movido."),
    GROUPS_CANNOT_BE_CREATED_AT_THE_MOMENT(     State.FAILURE, true,  "> ❌ Grupos não estão sendo criados no momento! Para mais informações, entre em contato com algum staff."),
    TICKETS_CANNOT_BE_CREATED_AT_THE_MOMENT(    State.FAILURE, true,  "> ❌ Tickets não estão sendo abertos no momento! Para mais informações, entre em contato com algum staff."),
    STAFF_CANNOT_CREATE_TICKETS(                State.FAILURE, true,  "> ❌ Staffs não podem abrir tickets."),
    TICKET_NOT_FOUND(                           State.FAILURE, true,  "> ❌ Ticket não encontrado."),
    GROUP_ALREADY_HAS_THE_PROVIDED_CHANNEL(     State.FAILURE, true,  "O grupo já possui o tipo de canal fornecido."),
    NAME_TOO_SHORT(                             State.FAILURE, true,  "Nomes precisam ter %d ou mais caracteres."),
    YOU_CANNOT_PUNISH_YOURSELF(                 State.FAILURE, true,  "Você não pode punir você mesmo."),
    YOU_CANNOT_BAN_YOURSELF(                    State.FAILURE, true,  "Você não pode banir você mesmo."),
    YOU_CANNOT_KICK_YOURSELF(                   State.FAILURE, true,  "Você não pode expulsar você mesmo."),
    YOUR_GROUP_DOES_NOT_HAVE_TEXT_CHANNEL(      State.FAILURE, true,  "Seu grupo não possui um canal de texto."),
    FAILED_TO_VALIDATE_PASSPHRASE(              State.FAILURE, true,  "Não foi possível validar a frase de confirmação."),
    INCORRECT_CONFIRMATION_VALUE(               State.FAILURE, true,  "O valor de confirmação está incorreto."),
    INVALID_DATE_FORMAT(                        State.FAILURE, true,  "A data fornecida não segue o padrão esperado ou se refere à um momento inexistente! Por favor, utilize o padrão informado."),
    DATETIME_MUST_BE_IN_THE_FUTURE(             State.FAILURE, false, "A data fornecida precisa estar no futuro! Fornecido: `%s`."),
    COULD_NOT_ADD_BIRTHDAY(                     State.FAILURE, true,  "Não foi possível salvar aniversário! Verifique console para mais informações sobre o erro."),
    USER_IS_NOT_IN_BIRTHDAY_LIST(               State.FAILURE, true,  "O usuário não está na lista de aniversariantes."),
    GROUP_NOT_FOUND(                            State.FAILURE, true,  "Grupo não encontrado."),
    PASSPHRASES_MISMATCH(                       State.FAILURE, true,  "Frase confirmatória está incorreta."),
    CHANNEL_MUST_HAVE_ONE_NATURAL_MESSAGE(      State.FAILURE, true,  "O canal precisa ter pelo menos uma mensagem normal."),
    CONFIRMATION_CODE_MISMATCH(                 State.FAILURE, true,  "Código incorreto."),
    GROUP_BOT_NOT_FOUND(                        State.FAILURE, true,  "Bot não encontrado."),
    GROUP_ROLE_NOT_FOUND(                       State.FAILURE, true,  "Cargo para o grupo `%s` não foi encontrado."),
    COULD_NOT_REMOVE_BIRTHDAY(                  State.FAILURE, true,  "Não foi possível remover o aniversário do usuário fornecido."),
    NO_RESULT_FOUND(                            State.FAILURE, true,  "Nenhum resultado encontrado."),
    INVALID_VALUE_PROVIDED(                     State.FAILURE, true,  "Valor inválido fornecido: `%s`."),
    INVALID_ID_PROVIDED(                        State.FAILURE, true,  "O ID fornecido é inválido: `%s`."),
    GROUP_CHANNELS_CANNOT_BE_CREATED(           State.FAILURE, true,  "Canais para grupos não estão sendo criados no momento."),
    VALUE_IS_LESS_OR_EQUAL_TO_ZERO(             State.FAILURE, true,  "O valor fornecido não pode ser menor ou igual a zero."),
    CANNOT_MARRY_TO_USER(                       State.FAILURE, true,  "Você não pode se casar com você mesmo ou com outros bots."),
    PENDING_PROPOSAL(                           State.FAILURE, true,  "Você ou esta pessoa já têm uma proposta de casamento enviada para o outro! Por favor, aguarde que seja aceito/recusado. Ou se foi você que enviou, você pode cancelar com `/marriage cancel`."),
    ALREADY_MARRIED_TO_USER(                    State.FAILURE, true,  "Você já está casado(a) com %s!"),
    USERS_CANNOT_HAVE_MULTIPLE_GROUPS(          State.FAILURE, true,  "Cada usuário pode ter até, no máximo, 1 grupo."),
    ISSUER_HIT_MARRIAGE_LIMIT(                  State.FAILURE, true,  "Você já chegou no limite de casamentos."),
    TARGET_HIT_MARRIAGE_LIMIT(                  State.FAILURE, true,  "O membro que você quer se casar já chegou no limite de casamentos."),
    MARRIAGE_INSUFFICIENT_BALANCE(              State.FAILURE, true,  "Saldo insuficiente! Ambos precisam ter `$%s` inicialmente para se casar."),
    CHANNEL_NOT_FOUND(                          State.FAILURE, true,  "Canal não encontrado."),
    TEXT_CHANNEL_NOT_FOUND(                     State.FAILURE, true,  "Canal de texto não encontrado."),
    VOICE_CHANNEL_NOT_FOUND(                    State.FAILURE, true,  "Canal de voz não encontrado."),
    MESSAGE_NOT_FOUND(                          State.FAILURE, true,  "Mensagem não encontrada."),
    COULD_NOT_CONVERT_DATA_FROM_FILE(           State.FAILURE, true,  "Não foi possível executar conversão dos dados! Verifique o formato do arquivo `%s` e tente novamente."),
    COMMAND_IS_ALREADY_RUNNING(                 State.FAILURE, true,  "Este comando já está em execução! Por favor, aguarde."),
    CHANNEL_CATEGORY_NOT_FOUND(                 State.FAILURE, true,  "A categoria do canal não foi encontrada."),
    CANNOT_UPDATE_SELF_MARRIAGE_DATE(           State.FAILURE, true,  "Você não pode alterar a data de um casamento de você com você mesmo."),
    PROVIDED_USERS_ARE_NOT_MARRIED(             State.FAILURE, true,  "Os usuários fornecidos não estão casados."),
    NO_ACTIVE_PROPOSAL_SENT_TO_USER(            State.FAILURE, true,  "Você não tem nenhuma proposta de casamento enviada à %s."),
    NO_INCOME_PROPOSAL_FROM_USER(               State.FAILURE, true,  "%s não te mandou nenhum pedido de casamento."),
    CANNOT_ACCEPT_SELF_MARRIAGE_PROPOSAL(       State.FAILURE, true,  "Você não pode aceitar uma proposta de casamento enviada por você mesmo."),
    INVALID_HEX_PROVIDED(                       State.FAILURE, true,  "Cores em HEX precisam conter exatamente `6` dígitos, opcionalmente acompanhados de um `#` (hashtag) no início.\nUse de auxílio: [__Google — Colour Picker__](<https://g.co/kgs/YKFnVZZ>)."),
    ROLE_NOT_FOUND(                             State.FAILURE, true,  "Cargo não encontrado."),
    ROLES_NOT_FOUND_TO_BACKUP(                  State.FAILURE, true,  "Nenhum cargo foi encontrado no backup."),
    ROLE_NOT_FOUND_BY_ID(                       State.FAILURE, true,  "Nenhum cargo encontrado para o id `%s`."),
    INSUFFICIENT_BALANCE_VALUE(                 State.FAILURE, true,  "> ❌ Você não tem saldo suficiente para esta operação!\nFalta: `$%s`."),
    INSUFFICIENT_BALANCE(                       State.FAILURE, true,  "> ❌ Saldo insuficiente."),
    MEMBER_INSUFFICIENT_BALANCE(                State.FAILURE, true,  "> ❌ O saldo de <@%d> é insuficiente."),
    IP_NOT_FOUND(                               State.FAILURE, true,  "IP não encontrado."),
    ISSUER_NOT_IN_VOICE_CHANNEL(                State.FAILURE, true,  "Você não está conectado em nenhum canal de voz."),
    TARGET_MAY_NOT_BE_A_BOT(                    State.FAILURE, true,  "O usuário fornecido não pode ser um bot."),
    INVALID_IP_ADDRESS_FORMAT(                  State.FAILURE, true,  "O IP `%s` é inválido."),
    INCORRECT_CHANNEL_OF_USAGE(                 State.FAILURE, true,  "Canal incorreto."),
    POLL_NOT_FOUND(                             State.FAILURE, true,  "Enquete não encontrada."),
    FILE_TOO_LARGE(                             State.FAILURE, true,  "O arquivo é grande demais, máximo: 25MB."),
    POLL_ALREADY_CLOSED_BY(                     State.FAILURE, true,  "Essa enquete já foi fechada por <@%d> em <t:%d>."),
    POLL_IS_NOT_CLOSED(                         State.FAILURE, true,  "Essa enquete não está fechada."),
    BASE_GROUP_ROLE_NOT_FOUND(                  State.FAILURE, true,  "Cargo base necessário não foi encontrado."),
    OWNER_MEMBER_NOT_FOUND(                     State.FAILURE, true,  "O dono fornecido não foi encontrado no servidor."),
    NO_GROUP_CHANNEL_FOUND(                     State.FAILURE, true,  "Nenhum canal de grupo encontrado."),

    COULD_NOT_EXECUTE_SUCH_OPERATION(           State.FAILURE, true,  "Não foi possível completar a operação.");

    private final boolean tick;
    private final State state;
    private final String data;

    Status(State state, boolean tick, String data) {
        this.state = state;
        this.tick = tick;
        this.data = data;
    }

    Status(State state, boolean tick) {
        this(state, tick, null);
    }

    public InteractionResult args(Object... args) {
        return new ResultData(this, args);
    }

    @Override
    public InteractionResult setEphm(boolean ephm) {
        return new ResultData(this, ResultData.EMPTY_ARGS, ephm);
    }

    @Override
    public Status getStatus() {
        return this;
    }

    @Override
    public String getContent() {
        return this.data;
    }

    @Override
    public boolean isEphemeral() {
        return this.state == State.FAILURE;
    }

    @Override
    public boolean ticksCooldown() {
        return this.tick;
    }

    @Override
    public boolean isOk() {
        return this.state == State.OK;
    }

    enum State {
        OK,
        PROCESSING,
        FAILURE
    }
}