package ofc.bot.domain.entity.enums;

import java.util.List;

import static ofc.bot.domain.entity.enums.ResourceType.*;

/**
 * This enum references the types of policies for the
 * table {@link ofc.bot.domain.tables.EntitiesPoliciesTable EntitiesPoliciesTable}.
 */
public enum PolicyType {
    EXEMPT_MARRIAGE_FEE(        of(USER), "Insentar das Taxas de Casamento."),
    HIDE_BIRTHDAY_AGE(          of(USER), "Ocultar a data de Nascimento."),
    HIDE_FROM_STAFF_LIST(       of(USER), "Ocultar da Lista de Staffs."),
    BLOCK_XP_GAINS(             of(USER, ROLE, CHANNEL), "Bloqueie os ganhos de XP."),
    BYPASS_WORD_BLOCKER(        of(USER, ROLE, CHANNEL), "Ignorar na moderação de palavras bloqueadas."),
    BYPASS_MASS_EMOJI_BLOCKER(  of(USER, ROLE, CHANNEL), "Ignorar na moderação de excesso de emojis."),
    BYPASS_MASS_MENTION_BLOCKER(of(USER, ROLE, CHANNEL), "Ignorar na moderação de marcações em excesso."),
    BYPASS_LINKS_BLOCKER(       of(USER, ROLE, CHANNEL), "Ignorar na moderação de links."),
    BYPASS_REPEATS_BLOCKER(     of(USER, ROLE, CHANNEL), "Ignorar na moderação de repetições de texto."),
    BYPASS_INVITES_BLOCKER(     of(USER, ROLE, CHANNEL), "Ignorar na moderação do envio de convites."),
    ALLOW_DOMAIN(               of(LINK), "Links permitidos.");

    private final List<ResourceType> supportedTypes;
    private final String description;

    PolicyType(List<ResourceType> supportedTypes, String desc) {
        this.supportedTypes = supportedTypes;
        this.description = desc;
    }

    private static List<ResourceType> of(ResourceType... types) {
        return List.of(types);
    }

    public String getDescription() {
        return this.description;
    }

    public List<ResourceType> getSupportedTypes() {
        return this.supportedTypes;
    }

    public boolean isSupported(ResourceType type) {
        return this.supportedTypes.contains(type);
    }

    public static List<PolicyType> getAutomods() {
        return List.of(
                BYPASS_WORD_BLOCKER,
                BYPASS_MASS_EMOJI_BLOCKER,
                BYPASS_MASS_MENTION_BLOCKER,
                BYPASS_LINKS_BLOCKER,
                BYPASS_REPEATS_BLOCKER,
                BYPASS_INVITES_BLOCKER
        );
    }
}
