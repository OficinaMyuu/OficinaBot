package ofc.bot.handlers.interactions.commands.contexts;

import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public interface OptionsContainer {
    boolean hasOption(String name);

    boolean hasOptions();

    <T> T getOption(@NotNull String name, T fallback, @NotNull Function<? super OptionMapping, ? extends T> resolver);

    @Nullable
    default <T> T getOption(@NotNull String name, @NotNull Function<? super OptionMapping, ? extends T> resolver) {
        return getOption(name, null, resolver);
    }

    @NotNull
    default <T> List<T> collectOptions(@NotNull Function<? super OptionMapping, T> resolver, @NotNull String... names) {
        return Arrays.stream(names)
                .map(name -> getOption(name, resolver))
                .filter(Objects::nonNull)
                .toList();
    }

    @NotNull
    default <T> T getSafeOption(@NotNull String name, @NotNull Function<? super OptionMapping, ? extends T> resolver) {
        T data = getOption(name, resolver);

        if (data == null)
            throwNullOptionException(name);

        return data;
    }

    default <T extends Enum<T>> T getEnumOption(@NotNull String name, T fallback, @NotNull Class<T> type) {
        T opt = getEnumOption(name, type);
        return opt == null ? fallback : opt;
    }

    @NotNull
    default <T extends Enum<T>> T getSafeEnumOption(@NotNull String name, @NotNull Class<T> type) {
        String opt = getSafeOption(name, OptionMapping::getAsString);
        T value = getEnumOption(name, type);

        if (value == null)
            throw new IllegalStateException(
                    "Could not find a valid option for argument \"" + name + "\", value " + opt +
                            " is not in " + Arrays.toString(type.getEnumConstants())
            );

        return value;
    }

    @Nullable
    default <T extends Enum<T>> T getEnumOption(@NotNull String name, @NotNull Class<T> type) {
        String opt = getOption(name, OptionMapping::getAsString);

        if (opt == null) return null;

        for (T en : type.getEnumConstants()) {
            if (en.name().equals(opt)) return en;
        }
        return null;
    }

    @Contract("_ -> fail")
    private void throwNullOptionException(String opt) {
        throw new IllegalStateException("The option \"" + opt + "\" was expected to be present but is missing. " +
                "This could be due to the option not being marked as required or a naming mismatch. " +
                "It is unlikely to be a Discord-related issue."
        );
    }
}