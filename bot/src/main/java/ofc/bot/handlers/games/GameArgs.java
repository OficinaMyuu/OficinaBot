package ofc.bot.handlers.games;

import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

public class GameArgs {
    public static final GameArgs EMPTY = new GameArgs();
    private final Object[] args;

    public GameArgs(@NotNull Object... args) {
        Checks.notNull(args, "args");
        this.args = args;
    }

    public int size() {
        return this.args.length;
    }

    @NotNull
    public <T> T get(int index) {
        T value = find(index);

        if (value == null)
            throw new NoSuchElementException("No value found at index " + index);

        return value;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T find(int index) {
        Object val = args[index];
        return val == null ? null : (T) val;
    }
}