package ofc.bot.handlers.interactions;

import ofc.bot.handlers.TemporaryStorage;

import java.util.*;

public final class InteractionMemoryManager {
    private static final InteractionMemoryManager INSTANCE = new InteractionMemoryManager();
    private final TemporaryStorage<EntityContext<?, ?>> contexts = new TemporaryStorage<>();
    private final Map<String, List<InteractionListener<?>>> subscribers = new HashMap<>();

    private InteractionMemoryManager() {}

    public static InteractionMemoryManager getManager() {
        return INSTANCE;
    }

    public void save(EntityContext<?, ?>... contexts) {
        for (EntityContext<?, ?> ctx : contexts) {
            ctx.checkFields();

            String id = ctx.getId();
            this.contexts.put(id, ctx, ctx.getValidityMillis());
        }
    }

    public void removeListener(InteractionListener<?> ls) {
        subscribers.values().forEach(l -> l.remove(ls));
    }

    public void removeListeners(String scope) {
        subscribers.remove(scope);
    }

    public void registerListeners(InteractionListener<?>... listeners) {
        for (InteractionListener<?> ls : listeners) {
            String scope = ls.getScope();

            List<InteractionListener<?>> scopeList = this.subscribers.getOrDefault(scope, new ArrayList<>());
            scopeList.add(ls);
            this.subscribers.put(scope, scopeList);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends InteractionListener<?>> List<T> getListeners(String scope) {
        return (List<T>) this.subscribers.getOrDefault(scope, List.of());
    }

    @SuppressWarnings("unchecked")
    public <T extends EntityContext<?, ?>> T get(String id) {
        return (T) this.contexts.find(id);
    }

    public void remove(String id) {
        this.contexts.remove(id);
    }
}