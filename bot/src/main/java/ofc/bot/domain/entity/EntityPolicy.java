package ofc.bot.domain.entity;

import ofc.bot.domain.entity.enums.ResourceType;
import ofc.bot.domain.entity.enums.PolicyType;
import ofc.bot.domain.tables.EntitiesPoliciesTable;
import ofc.bot.util.Bot;

import java.util.function.Function;

public class EntityPolicy extends OficinaRecord<EntityPolicy> {
    private static final EntitiesPoliciesTable ENTITIES_POLICIES = EntitiesPoliciesTable.ENTITIES_POLICIES;

    public EntityPolicy() {
        super(ENTITIES_POLICIES);
    }

    public EntityPolicy(PolicyType type, ResourceType resourceType, Object resource, long createdAt) {
        this();
        set(ENTITIES_POLICIES.RESOURCE, String.valueOf(resource));
        set(ENTITIES_POLICIES.RESOURCE_TYPE, resourceType.name());
        set(ENTITIES_POLICIES.POLICY_TYPE, type.toString());
        set(ENTITIES_POLICIES.CREATED_AT, createdAt);
    }

    public EntityPolicy(PolicyType type, ResourceType resourceType, Object resource) {
        this(type, resourceType, resource, Bot.unixNow());
    }

    public int getId() {
        return get(ENTITIES_POLICIES.ID);
    }

    public <T> T getResource(Function<String, T> resolver) {
        String res = get(ENTITIES_POLICIES.RESOURCE);
        return res == null ? null : resolver.apply(res);
    }

    public ResourceType getResourceType() {
        String type = get(ENTITIES_POLICIES.RESOURCE_TYPE);
        return ResourceType.valueOf(type);
    }

    public PolicyType getPolicyType() {
        String type = get(ENTITIES_POLICIES.POLICY_TYPE);
        return PolicyType.valueOf(type);
    }

    public long getTimeCreated() {
        return get(ENTITIES_POLICIES.CREATED_AT);
    }
}