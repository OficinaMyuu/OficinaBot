package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.handlers.requests.Route;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.regex.Pattern;

@DiscordCommand(name = "ip")
public class IPLookupCommand extends SlashCommand {
    private static final String REGEX = "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b";
    private static final Color EMBED_COLOR = new Color(114, 222, 64);

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        String ipInput = ctx.getSafeOption("ip", OptionMapping::getAsString);
        Guild guild = ctx.getGuild();

        if (!Pattern.matches(REGEX, ipInput))
            return Status.INVALID_IP_ADDRESS_FORMAT.args(ipInput);

        ctx.ack();
        IPData ipData = getIpData(ipInput);

        if (ipData == null)
            return Status.IP_NOT_FOUND;

        MessageEmbed embed = embed(guild, ipData);
        return ctx.replyEmbeds(embed);
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Coleta informações aproximadas de um IP.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "ip", "O IP a executar o lookup.", true)
        );
    }

    private MessageEmbed embed(Guild guild, IPData ipData) {
        EmbedBuilder builder = new EmbedBuilder();

        builder
                .setTitle("🗺 IP Lookup")
                .setDescription("Mostrando informações do IP: `" + ipData.query + "`.")
                .setColor(EMBED_COLOR)
                .addField("🌎 País", ipData.country + " (" + ipData.countryCode + ")", true)
                .addField("📌 Região", ipData.regionName, true)
                .addField("🏙 Cidade", ipData.city, true)
                .addField("🕒 Timezone", ipData.timezone, true)
                .addField("📡 Provedor", ipData.isp, true)
                .setFooter(guild.getName(), guild.getIconUrl());

        if (ipData.zip != null && !ipData.zip.isBlank())
            builder.addField("📬 Código Postal", ipData.zip, true);

        return builder.build();
    }

    private IPData getIpData(String ip) {
        IPData value = Route.IPs.GET_IP_INFO
                .create(ip)
                .send()
                .json(IPData.class);

        return value.status.equals("success")
                ? value
                : null;
    }

    private record IPData(
            String query,
            String status,
            String country,
            String countryCode,
            String regionName,
            String city,
            String timezone,
            String zip,
            String isp,
            String org
    ) {}
}