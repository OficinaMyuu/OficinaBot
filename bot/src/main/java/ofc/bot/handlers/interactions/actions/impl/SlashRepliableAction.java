package ofc.bot.handlers.interactions.actions.impl;

import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;

public class SlashRepliableAction extends GenericInteractionRepliableAction<SlashCommandInteraction> {

    public SlashRepliableAction(SlashCommandInteraction itr) {
        super(itr);
    }
}