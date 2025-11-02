package ofc.bot.listeners.discord.interactions.buttons.groups;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.util.GroupHelper;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InteractionHandler(scope = Scopes.Group.REMOVE_MEMBER, autoResponseType = AutoResponseType.THINKING_EPHEMERAL)
public class GroupMemberRemoveHandler implements InteractionListener<ButtonClickContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupMemberRemoveHandler.class);

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        Guild guild = ctx.getGuild();
        OficinaGroup group = ctx.get("group");
        long targetId = ctx.get("target_id");
        long groupRoleId = group.getRoleId();
        Role groupRole = guild.getRoleById(groupRoleId);

        if (groupRole == null)
            return Status.GROUP_ROLE_NOT_FOUND;

        guild.removeRoleFromMember(UserSnowflake.fromId(targetId), groupRole).queue(v -> {
            ctx.reply(Status.MEMBER_SUCCESSFULLY_REMOVED_FROM_GROUP.args(targetId));
            handleDisconnect(guild, group, targetId);

            GroupHelper.registerMemberRemoved(group);
        }, (err) -> {
            LOGGER.error("Could not remove role &{} from member @{}", groupRoleId, targetId);
            ctx.reply(Status.COULD_NOT_EXECUTE_SUCH_OPERATION);
        });
        ctx.disable();
        return Status.OK;
    }

    private void handleDisconnect(Guild guild, OficinaGroup group, long userId) {
        guild.retrieveMemberById(userId).queue((member) -> {
            GuildVoiceState voice = member.getVoiceState();
            AudioChannelUnion memberChannel = voice == null ? null : voice.getChannel();
            VoiceChannel groupChannel = group.getVoiceChannel();

            if (memberChannel == null || groupChannel == null) return;

            if (memberChannel.getIdLong() == groupChannel.getIdLong()) {
                guild.kickVoiceMember(member).queue();
            }
        }, (err) -> LOGGER.error("Could not find member @{} to disconnect, sad", userId));
    }
}
