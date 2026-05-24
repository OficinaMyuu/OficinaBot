package ofc.bot.listeners.discord.interactions.menus;

import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@DiscordEventHandler
public class ChoosableRolesListener extends ListenerAdapter {

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent e) {
        String menuId = e.getComponentId();
        Member member = e.getMember();

        if (member == null || !"choosable_roles".equals(menuId)) return;

        Guild guild = member.getGuild();
        StringSelectMenu menu = e.getSelectMenu();
        List<SelectOption> selected = e.getSelectedOptions();
        List<SelectOption> unselected = menu.getOptions().stream().filter(so -> !selected.contains(so)).toList();
        List<Role> toAdd = intoRoles(member, selected, (r) -> !member.getRoles().contains(r));
        List<Role> toRem = intoRoles(member, unselected, (r) -> member.getRoles().contains(r));
        String response = getResponse(toAdd.size(), toRem.size());

        // The reason we are not using Guild#modifyMemberRoles(member, toAdd, toRem) here
        // is because Discord restricts bots from modifying members who have roles equal to or higher
        // than the bot's highest role. This means that attempting to modify roles in bulk
        // through this method would fail for such members.
        //
        // However, Discord does allow bots to individually add or remove roles from members
        // with higher or equal roles, even though bulk modification is restricted.
        //
        // As a workaround, instead of making a single request to modify multiple roles at once,
        // we loop through each role and call the appropriate add/remove method.
        // This ensures that the bot can still manage roles for higher-ranked members
        // by handling each addition and removal as an independent request.
        for (Role add : toAdd) {
            guild.addRoleToMember(member, add).queue();
        }

        for (Role rem : toRem) {
            guild.removeRoleFromMember(member, rem).queue();
        }

        e.reply(response).setEphemeral(true).queue();
    }

    // Resolves the selected options into roles and
    // returns only the roles that are found AND the `member` does not have.
    private List<Role> intoRoles(Member member, List<SelectOption> opts, Predicate<Role> filter) {
        Guild guild = member.getGuild();
        return opts.stream()
                .map(opt -> guild.getRoleById(opt.getValue()))
                .filter(Objects::nonNull)
                .filter(filter)
                .toList();
    }

    private String getResponse(int added, int removed) {
        return String.format("""
                ## ✨ Prontinho!
                
                - **Cargos Adicionados:** `%s`.
                - **Cargos Removidos:** `%s`.
                """, added, removed);
    }
}