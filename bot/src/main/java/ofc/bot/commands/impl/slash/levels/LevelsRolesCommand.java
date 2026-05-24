package ofc.bot.commands.impl.slash.levels;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import ofc.bot.domain.entity.LevelRole;
import ofc.bot.domain.sqlite.repository.LevelRoleRepository;
import ofc.bot.handlers.interactions.commands.Cooldown;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.handlers.requests.RequestMapper;
import ofc.bot.handlers.requests.Route;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

@DiscordCommand(name = "levels-roles")
public class LevelsRolesCommand extends SlashCommand {
    private static final Color BACKGROUND_COLOR = new Color(45, 44, 60);
    private final LevelRoleRepository lvlRoleRepo;

    public LevelsRolesCommand(LevelRoleRepository lvlRoleRepo) {
        this.lvlRoleRepo = lvlRoleRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        List<LevelRole> roles = lvlRoleRepo.findAll();
        Guild guild = ctx.getGuild();

        if (roles.isEmpty()) // Hm???
            return Status.NO_LEVEL_ROLE_FOUND;

        ctx.ack();
        byte[] img = getRolesImage(guild, roles);
        if (img.length == 0)
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;

        return ctx.replyFile(img, "levels.png");
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Mostra o cargo para cada nível.";
    }

    @NotNull
    @Override
    public Cooldown getCooldown() {
        return Cooldown.of(10, TimeUnit.SECONDS);
    }

    public static byte[] getRolesImage(Guild guild, List<LevelRole> roles) {
        String key = Bot.get("oficina.aws.api.key");
        DataObject payload = finalizeData(guild, roles);
        RequestMapper result = Route.Images.CREATE_ROLES_CARD.create()
                .addHeader("x-api-key", key)
                .setBody(payload)
                .send();

        if (result.getStatusCode() != 200)
            return new byte[0];

        DataObject resp = result.asDataObject();
        String cardImage = resp.getString("image");
        return Base64.getDecoder().decode(cardImage);
    }

    private static DataObject finalizeData(Guild guild, List<LevelRole> roles) {
        DataObject guildDTO = DataObject.empty()
                .put("name", guild.getName())
                .put("icon_url", guild.getIconUrl());

        DataObject payload = DataObject.empty()
                .put("levels", DataArray.empty())
                .put("guild", guildDTO)
                .put("background_color", Bot.toRGB(BACKGROUND_COLOR));

        for (LevelRole lvlRole : roles) {
            Role role = lvlRole.toRole();
            if (role == null) continue;

            DataObject roleDTO = DataObject.empty()
                    .put("name", role.getName())
                    .put("color", role.getColorRaw())
                    .put("level", lvlRole.getLevel());

            payload.getArray("levels")
                    .add(roleDTO);
        }
        return payload;
    }
}