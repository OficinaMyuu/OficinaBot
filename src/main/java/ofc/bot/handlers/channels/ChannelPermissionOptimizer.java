package ofc.bot.handlers.channels;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ChannelPermissionOptimizer {
    public AnalysisResult analyze(GuildChannel channel, List<Member> members) {
        Guild guild = channel.getGuild();
        long publicRoleId = guild.getPublicRole().getIdLong();
        boolean audioAccess = isAudioAccess(channel.getType());
        List<OverrideState> overrides = snapshotOverrides(channel.getPermissionContainer(), guild);
        List<MemberSnapshot> memberSnapshots = snapshotMembers(channel, members);
        OptimizationRequest request = new OptimizationRequest(publicRoleId, audioAccess, overrides, memberSnapshots);

        return analyze(request);
    }

    static AnalysisResult analyze(OptimizationRequest request) {
        List<OverrideState> original = copyOverrides(request.overrides());
        ValidationResult validation = validateCurrentState(request, original);

        if (!validation.ok()) {
            return AnalysisResult.invalid(
                    request.members().size(),
                    original.size(),
                    computeSignature(original),
                    validation.reason()
            );
        }

        List<OverrideState> working = copyOverrides(original);
        int simulatedCandidates = 0;

        for (int i = 0; i < working.size(); i++) {
            OverrideState override = working.get(i);
            long[] allowBits = permissionBitsOf(override.allowRaw());

            for (long bit : allowBits) {
                OverrideState candidate = override.withAllowRaw(override.allowRaw() & ~bit);
                working.set(i, candidate);
                simulatedCandidates++;

                if (preservesAllMembers(request, working)) {
                    override = candidate;
                    continue;
                }

                working.set(i, override);
            }

            long[] denyBits = permissionBitsOf(override.denyRaw());
            for (long bit : denyBits) {
                OverrideState candidate = override.withDenyRaw(override.denyRaw() & ~bit);
                working.set(i, candidate);
                simulatedCandidates++;

                if (preservesAllMembers(request, working)) {
                    override = candidate;
                    continue;
                }

                working.set(i, override);
            }
        }

        List<OverrideChange> changes = diffOverrides(original, working);
        return AnalysisResult.valid(
                request.members().size(),
                original.size(),
                simulatedCandidates,
                computeSignature(original),
                changes
        );
    }

    private static ValidationResult validateCurrentState(OptimizationRequest request, List<OverrideState> overrides) {
        for (MemberSnapshot member : request.members()) {
            if (member.immuneToOverrides()) {
                continue;
            }

            SimulationResult simulated = simulate(member, overrides, request.publicRoleId(), request.audioAccess());
            if (simulated.explicitPermissionsRaw() != member.expectedExplicitPermissionsRaw()) {
                return ValidationResult.failure("A simulação local não bateu com o estado atual do JDA.");
            }

            if (simulated.hasAccess() != member.expectedAccess()) {
                return ValidationResult.failure("A simulação local não bateu com o acesso atual calculado pelo JDA.");
            }
        }
        return ValidationResult.success();
    }

    private static boolean preservesAllMembers(OptimizationRequest request, List<OverrideState> overrides) {
        for (MemberSnapshot member : request.members()) {
            if (member.immuneToOverrides()) {
                continue;
            }

            SimulationResult simulated = simulate(member, overrides, request.publicRoleId(), request.audioAccess());
            if (simulated.explicitPermissionsRaw() != member.expectedExplicitPermissionsRaw()) {
                return false;
            }

            if (simulated.hasAccess() != member.expectedAccess()) {
                return false;
            }
        }
        return true;
    }

    private static SimulationResult simulate(
            MemberSnapshot member, List<OverrideState> overrides, long publicRoleId, boolean audioAccess
    ) {
        if (member.immuneToOverrides()) {
            return new SimulationResult(member.expectedExplicitPermissionsRaw(), member.expectedAccess());
        }

        long permissions = member.guildExplicitPermissionsRaw();
        long aggregatedAllow = 0L;
        long aggregatedDeny = 0L;

        for (OverrideState override : overrides) {
            if (override.holderType() == HolderType.ROLE && override.holderId() == publicRoleId) {
                permissions = applyOverride(permissions, override.allowRaw(), override.denyRaw());
                continue;
            }

            if (override.holderType() == HolderType.ROLE && member.roleIds().contains(override.holderId())) {
                aggregatedAllow |= override.allowRaw();
                aggregatedDeny |= override.denyRaw();
                continue;
            }

            if (override.holderType() == HolderType.MEMBER && override.holderId() == member.memberId()) {
                permissions = applyOverride(permissions, aggregatedAllow, aggregatedDeny);
                permissions = applyOverride(permissions, override.allowRaw(), override.denyRaw());
                return new SimulationResult(permissions, hasAccess(permissions, audioAccess));
            }
        }

        permissions = applyOverride(permissions, aggregatedAllow, aggregatedDeny);
        return new SimulationResult(permissions, hasAccess(permissions, audioAccess));
    }

    private static long applyOverride(long permissions, long allowRaw, long denyRaw) {
        permissions &= ~denyRaw;
        permissions |= allowRaw;
        return permissions;
    }

    private static boolean hasAccess(long permissions, boolean audioAccess) {
        boolean hasView = (permissions & Permission.VIEW_CHANNEL.getRawValue()) != 0L;
        if (!hasView) {
            return false;
        }

        if (!audioAccess) {
            return true;
        }

        return (permissions & Permission.VOICE_CONNECT.getRawValue()) != 0L;
    }

    private static List<OverrideChange> diffOverrides(List<OverrideState> before, List<OverrideState> after) {
        List<OverrideChange> changes = new ArrayList<>();

        for (int i = 0; i < before.size(); i++) {
            OverrideState oldOverride = before.get(i);
            OverrideState newOverride = after.get(i);
            long removedAllowed = oldOverride.allowRaw() & ~newOverride.allowRaw();
            long removedDenied = oldOverride.denyRaw() & ~newOverride.denyRaw();

            if (removedAllowed == 0L && removedDenied == 0L) {
                continue;
            }

            changes.add(new OverrideChange(oldOverride, newOverride, removedAllowed, removedDenied));
        }

        return changes;
    }

    private static List<OverrideState> snapshotOverrides(IPermissionContainer channel, Guild guild) {
        return channel.getPermissionOverrides().stream()
                .sorted(Comparator
                        .comparing((PermissionOverride override) -> override.isMemberOverride() ? 1 : 0)
                        .thenComparingLong(PermissionOverride::getIdLong))
                .map(override -> {
                    HolderType type = override.isMemberOverride() ? HolderType.MEMBER : HolderType.ROLE;
                    long holderId = override.getIdLong();

                    return new OverrideState(
                            holderId,
                            type,
                            override.getAllowedRaw(),
                            override.getDeniedRaw(),
                            resolveHolderLabel(override, guild)
                    );
                })
                .toList();
    }

    private static List<MemberSnapshot> snapshotMembers(GuildChannel channel, List<Member> members) {
        List<MemberSnapshot> snapshots = new ArrayList<>(members.size());

        for (Member member : members) {
            Set<Long> roleIds = new HashSet<>();
            for (Role role : member.getRoles()) {
                roleIds.add(role.getIdLong());
            }

            boolean immuneToOverrides = member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR);
            long explicitPermissionsRaw = Permission.getRaw(member.getPermissionsExplicit(channel));
            boolean hasAccess = member.hasAccess(channel);
            long guildExplicitPermissionsRaw = Permission.getRaw(member.getPermissionsExplicit());

            snapshots.add(new MemberSnapshot(
                    member.getIdLong(),
                    Set.copyOf(roleIds),
                    immuneToOverrides,
                    guildExplicitPermissionsRaw,
                    explicitPermissionsRaw,
                    hasAccess
            ));
        }

        return snapshots;
    }

    private static String resolveHolderLabel(PermissionOverride override, Guild guild) {
        if (override.isRoleOverride()) {
            Role role = override.getRole();
            if (role != null && role.getIdLong() == guild.getPublicRole().getIdLong()) {
                return "@everyone";
            }

            if (role != null) {
                return "Cargo " + role.getName();
            }

            return "Cargo " + override.getId();
        }

        Member member = override.getMember();
        if (member != null) {
            return "Membro " + member.getEffectiveName();
        }

        return "Membro " + override.getId();
    }

    private static List<OverrideState> copyOverrides(List<OverrideState> overrides) {
        return overrides.stream()
                .map(override -> new OverrideState(
                        override.holderId(),
                        override.holderType(),
                        override.allowRaw(),
                        override.denyRaw(),
                        override.holderLabel()
                ))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private static long[] permissionBitsOf(long raw) {
        return permissionsOf(raw).stream()
                .mapToLong(Permission::getRawValue)
                .toArray();
    }

    public static EnumSet<Permission> permissionsOf(long raw) {
        EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);

        for (Permission permission : Permission.values()) {
            long bit = permission.getRawValue();
            if (bit != 0L && (raw & bit) != 0L) {
                permissions.add(permission);
            }
        }

        return permissions;
    }

    public static String computeSignature(List<OverrideState> overrides) {
        StringBuilder builder = new StringBuilder(overrides.size() * 32);

        for (OverrideState override : overrides) {
            builder.append(override.holderType().name())
                    .append(':')
                    .append(override.holderId())
                    .append(':')
                    .append(override.allowRaw())
                    .append(':')
                    .append(override.denyRaw())
                    .append(';');
        }

        return builder.toString();
    }

    private static boolean isAudioAccess(ChannelType type) {
        return type == ChannelType.VOICE || type == ChannelType.STAGE;
    }

    public enum HolderType {
        ROLE,
        MEMBER
    }

    record OptimizationRequest(
            long publicRoleId,
            boolean audioAccess,
            List<OverrideState> overrides,
            List<MemberSnapshot> members
    ) {}

    record ValidationResult(boolean ok, String reason) {
        static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        static ValidationResult failure(String reason) {
            return new ValidationResult(false, reason);
        }
    }

    record SimulationResult(long explicitPermissionsRaw, boolean hasAccess) {}

    public record OverrideState(
            long holderId,
            HolderType holderType,
            long allowRaw,
            long denyRaw,
            String holderLabel
    ) {
        public OverrideState withAllowRaw(long newAllowRaw) {
            return new OverrideState(holderId, holderType, newAllowRaw, denyRaw, holderLabel);
        }

        public OverrideState withDenyRaw(long newDenyRaw) {
            return new OverrideState(holderId, holderType, allowRaw, newDenyRaw, holderLabel);
        }

        public boolean isEmpty() {
            return allowRaw == 0L && denyRaw == 0L;
        }
    }

    public record OverrideChange(
            OverrideState before,
            OverrideState after,
            long removedAllowedRaw,
            long removedDeniedRaw
    ) {
        public boolean deletesOverride() {
            return after.isEmpty();
        }
    }

    public record MemberSnapshot(
            long memberId,
            Set<Long> roleIds,
            boolean immuneToOverrides,
            long guildExplicitPermissionsRaw,
            long expectedExplicitPermissionsRaw,
            boolean expectedAccess
    ) {}

    public record AnalysisResult(
            boolean validCurrentState,
            int memberCount,
            int overrideCount,
            int simulatedCandidates,
            String originalSignature,
            String failureReason,
            List<OverrideChange> changes
    ) {
        public static AnalysisResult valid(
                int memberCount, int overrideCount, int simulatedCandidates, String originalSignature,
                List<OverrideChange> changes
        ) {
            return new AnalysisResult(true, memberCount, overrideCount, simulatedCandidates, originalSignature, null, List.copyOf(changes));
        }

        public static AnalysisResult invalid(
                int memberCount, int overrideCount, String originalSignature, String failureReason
        ) {
            return new AnalysisResult(false, memberCount, overrideCount, 0, originalSignature, failureReason, List.of());
        }

        public boolean hasChanges() {
            return !changes.isEmpty();
        }

        public int changedOverrideCount() {
            return changes.size();
        }

        public int removedPermissionCount() {
            int count = 0;
            for (OverrideChange change : changes) {
                count += permissionsOf(change.removedAllowedRaw()).size();
                count += permissionsOf(change.removedDeniedRaw()).size();
            }
            return count;
        }
    }
}
