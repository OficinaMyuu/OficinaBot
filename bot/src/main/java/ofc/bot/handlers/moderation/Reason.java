package ofc.bot.handlers.moderation;

import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Reason implements Iterable<String> {
    private final Collection<String> reasons;

    public Reason(boolean acceptsDuplicates) {
        this.reasons = acceptsDuplicates ? new HashSet<>() : new LinkedList<>();
    }

    public Reason() {
        this(false);
    }

    @NotNull
    public static Reason of(@NotNull String reason) {
        Checks.notNull(reason, "Reason");
        return new Reason().add(reason);
    }

    @NotNull
    public Reason add(@NotNull String reason) {
        Checks.notNull(reason, "Reason");
        reasons.add(reason);
        return this;
    }

    public int size() {
        return reasons.size();
    }

    public boolean isEmpty() {
        return reasons.isEmpty();
    }

    @Override
    @NotNull
    public String toString() {
        return String.join(", ", reasons);
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return this.reasons.iterator();
    }
}