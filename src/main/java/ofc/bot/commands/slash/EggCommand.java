package ofc.bot.commands.slash;

import ofc.bot.handlers.interactions.commands.Cooldown;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@DiscordCommand(name = "egg")
public class EggCommand extends SlashCommand {

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        return ctx.reply("🥚"); // fica na sua Yuna
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Responde com um ovo no chat.";
    }

    @NotNull
    @Override
    public Cooldown getCooldown() {
        // Seria /egg name, vou até deixar você escolher o cooldown, sim
        // Vou manter todos esses comentários no código
        // porque sou retardado.

        // Mas fica no histórico do GitHub, esses comentários NUNCA serão apagados
        // da história.

        // Medo
        return Cooldown.of(30, TimeUnit.SECONDS);
    }
}