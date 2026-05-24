package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.EntityPolicy;
import ofc.bot.domain.entity.enums.PolicyType;
import ofc.bot.domain.tables.EntitiesPoliciesTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Repository for {@link EntityPolicy} entity.
 */
public class EntityPolicyRepository extends Repository<EntityPolicy> {
    private static final EntitiesPoliciesTable ENTITIES_POLICIES = EntitiesPoliciesTable.ENTITIES_POLICIES;

    public EntityPolicyRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<EntityPolicy> getTable() {
        return ENTITIES_POLICIES;
    }

    public void deleteByPolicyAndResource(PolicyType type, Object resource) {
        ctx.deleteFrom(ENTITIES_POLICIES)
                .where(ENTITIES_POLICIES.POLICY_TYPE.eq(type.name()))
                .and(ENTITIES_POLICIES.RESOURCE.eq(String.valueOf(resource)))
                .execute();
    }

    public EntityPolicy findByPolicyAndResource(PolicyType type, Object resource) {
        return ctx.selectFrom(ENTITIES_POLICIES)
                .where(ENTITIES_POLICIES.POLICY_TYPE.eq(type.name()))
                .and(ENTITIES_POLICIES.RESOURCE.eq(String.valueOf(resource)))
                .fetchOne();
    }

    public void delete(EntityPolicy entity) {
        ctx.deleteFrom(ENTITIES_POLICIES)
                .where(ENTITIES_POLICIES.ID.eq(entity.getId()))
                .execute();
    }

    public boolean existsByTypeAndResource(PolicyType type, Object resource) {
        return ctx.fetchExists(ENTITIES_POLICIES,
                ENTITIES_POLICIES.POLICY_TYPE.eq(type.name())
                        .and(ENTITIES_POLICIES.RESOURCE.eq(String.valueOf(resource)))
        );
    }

    public <T> Set<T> findSetByType(PolicyType type, Function<String, T> mapper) {
        return findEntitiesIdsByType(type, mapper).stream().collect(Collectors.toUnmodifiableSet());
    }

    public List<EntityPolicy> findByTypes(PolicyType... types) {
        return findByTypes(List.of(types));
    }

    public List<EntityPolicy> findByTypes(List<PolicyType> types) {
        List<String> strTypes = types.stream().map(PolicyType::toString).toList();
        return ctx.selectFrom(ENTITIES_POLICIES)
                .where(ENTITIES_POLICIES.POLICY_TYPE.in(strTypes))
                .fetch();
    }

    public <T> List<T> findEntitiesIdsByType(PolicyType type, Function<String, T> resolver) {
        return ctx.select(ENTITIES_POLICIES.RESOURCE)
                .from(ENTITIES_POLICIES)
                .where(ENTITIES_POLICIES.POLICY_TYPE.eq(type.toString()))
                .fetchStreamInto(String.class)
                .map(resolver)
                .toList();
    }
}