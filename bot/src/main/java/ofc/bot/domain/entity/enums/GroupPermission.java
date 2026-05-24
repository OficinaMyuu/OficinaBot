package ofc.bot.domain.entity.enums;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.listeners.discord.moderation.AutoModerator;

public enum GroupPermission {

    /* Relies on Discord permissions */
    ADD_REACTIONS("Adicionar Reações", Permission.MESSAGE_ADD_REACTION),
    CRAETE_POLLS( "Criar Enquetes",    Permission.MESSAGE_SEND_POLLS),

    /* Relies on Oficina's AutoModeration system */
    SEND_BLOCKED_WORDS( "Enviar Palavrões",          PolicyType.BYPASS_WORD_BLOCKER),
    SEND_MASS_EMOJI(    "Enviar Emoji em Massa",     PolicyType.BYPASS_MASS_EMOJI_BLOCKER),
    SEND_MASS_MENTIONS( "Enviar Marcações em Massa", PolicyType.BYPASS_MASS_MENTION_BLOCKER),
    SEND_REAPEATED_TEXT("Enviar Texto Repetido",     PolicyType.BYPASS_REPEATS_BLOCKER),
    SEND_LINKS(         "Enviar Links",              PolicyType.BYPASS_LINKS_BLOCKER);

    private final String display;
    private final PolicyType policyType;
    private final Permission referencedPermission;

    GroupPermission(String display, PolicyType policy, Permission referencedPermision) {
        Checks.notEmpty(display, "Display");
        if (policy == null && referencedPermision == null)
            throw new IllegalArgumentException("PolicyType and Permission cannot be both null at the same time");

        this.display = display;
        this.policyType = policy;
        this.referencedPermission = referencedPermision;
    }

    GroupPermission(String display, PolicyType policy) {
        this(display, policy, null);
    }

    GroupPermission(String display, Permission referencedPermission) {
        this(display, null, referencedPermission);
    }

    public String getDisplay() {
        return this.display;
    }

    public PolicyType getPolicyType() {
        return this.policyType;
    }

    public Permission getReferencedPermission() {
        return this.referencedPermission;
    }

    /**
     * Checks if the permission is a Discord-based restriction.
     *
     * @return {@code true} if this is a real Discord {@link Permission},
     *         {@code false} otherwise (usually when its a database check like {@link AutoModerator}).
     */
    public boolean isDiscord() {
        return this.referencedPermission != null;
    }
}