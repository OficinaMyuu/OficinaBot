package ofc.bot.utils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.requests.RestAction;
import ofc.bot.exceptions.CommandPatternParsingException;
import ofc.bot.internal.BotData;

public class CommandParser {
    private static final String PREFIX = BotData.PREFIX;

    private CommandParser() {}

    public static String[] resolveArgs(String input) {

        String content = input.startsWith(PREFIX)
                ? input.substring(PREFIX.length()).strip()
                : input.strip();

        return content.split(" ");
    }

    public static RestAction<Member> findTarget(Guild guild, String input) {
        return guild.retrieveMemberById(getNumbers(input));
    }

    public static int findAge(String pattern) {

        String nums = getNumbers(pattern);

        try {
            return Integer.parseInt(nums);
        } catch (NumberFormatException e) {
            throw new CommandPatternParsingException(e);
        }
    }

    public static Roles findDevice(String pattern) {

        char value = pattern.charAt(pattern.length() - 1);

        return switch (value) {
            case 'p' -> Roles.DESKTOP;

            case 'm' -> Roles.MOBILE;

            default -> throw new CommandPatternParsingException("Could not parse device. Invalid pattern: " + pattern);
        };
    }

    public static Roles findGender(String pattern) {

        char value = pattern.charAt(0);

        return switch (value) {
            case 'f' -> Roles.FEMALE;

            case 'm' -> Roles.MALE;

            case 'n' -> Roles.NON_BINARY;

            default -> throw new CommandPatternParsingException("Could not parse gender. Invalid pattern: " + pattern);
        };
    }

    public static String getNumbers(String input) {
        return input.replaceAll("\\D+", "");
    }
}