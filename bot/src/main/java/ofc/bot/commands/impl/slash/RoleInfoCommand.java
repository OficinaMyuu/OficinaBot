package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.Bot;
import ofc.bot.util.OficinaEmbed;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@DiscordCommand(name = "roleinfo")
public class RoleInfoCommand extends SlashCommand {

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        ctx.ack();
        Guild guild = ctx.getGuild();
        Role role = ctx.getOption("role", OptionMapping::getAsRole);

        if (role == null)
            return Status.ROLE_NOT_FOUND;

        guild.findMembersWithRoles(role).onSuccess((members) -> {
            MessageEmbed embed = embed(members, role);
            ctx.replyEmbeds(embed);
        }).onError((e) -> {
            MessageEmbed embed = embed(List.of(), role);
            ctx.create()
                    .setContent("Não foi possível encontrar membros para o cargo, tente novamente.")
                    .setEmbeds(embed)
                    .send();
        });
        return Status.OK;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Informações gerais sobre um cargo.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.ROLE, "role", "O cargo a saber as informações.", true)
        );
    }

    @SuppressWarnings("ConstantConditions")
    private MessageEmbed embed(List<Member> members, Role role) {
        OficinaEmbed builder = new OficinaEmbed();
        int color = role.getColorRaw();
        long creation = role.getTimeCreated().toEpochSecond();
        List<Member> onlineMembers = members.stream().filter((m) -> m.getOnlineStatus() != OnlineStatus.OFFLINE).toList();
        RoleIcon icon = role.getIcon();
        Guild guild = role.getGuild();
        String memberCount = Bot.fmtNum(members.size());
        String onlineCount = Bot.fmtNum(onlineMembers.size());
        String colorField = getColorField(color);

        return builder.setTitle(role.getName())
                .setDesc("Informações do cargo <@&" + role.getIdLong() + ">.")
                .setColor(role.getColor())
                .addField("📅 Criação", "<t:" + creation + ">\n<t:" + creation + ":R>", true)
                .addField("💻 Role ID", "`" + role.getIdLong() + "`", true)
                .addField("🤖 Integração", role.isManaged() ? "`Sim`" : "`Não`", true)
                .addField((role.isMentionable() ? "🔔" : "🔕") + " Mencionável", role.isMentionable() ? "`Sim`" : "`Não`", true)
                .addField("📃 Mostrar Separadamente", role.isHoisted() ? "`Sim`" : "`Não`", true)
                .addField("🎨 Cor", colorField, true)
                .addField("👥 Membros", "Total: `" + memberCount + "`\nOnline: `" + onlineCount + "`", true)
                .addField("🔒 Permissões", stringifyPermissions(role), role.getPermissions().isEmpty())
                .setFooter(guild.getName(), guild.getIconUrl())
                .setThumbnailIf(icon != null, () -> icon.getIconUrl())
                .build();
    }

    private String getColorField(int rgb) {
        boolean hasColor = rgb != Role.DEFAULT_COLOR_RAW;
        String hexColor = String.format("`%s`", hasColor ? Bot.fmtColorHex(rgb) : "Nenhuma");
        Color color = new Color(hasColor ? rgb : 0);
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();

        return String.format("HEX: `%s`\nRGB: `%s, %s, %s`", hexColor, red, green, blue);
    }

    private String stringifyPermissions(Role role) {
        EnumSet<Permission> permissions = role.getPermissions();
        if (permissions.isEmpty()) return "`Nenhuma`";

        String fmtPerms = permissions.stream().map(Permission::getName).collect(Collectors.joining(", "));
        return String.format("```\n%s.\n```", fmtPerms);
    }
}