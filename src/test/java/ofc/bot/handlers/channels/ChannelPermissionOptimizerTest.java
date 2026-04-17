package ofc.bot.handlers.channels;

import net.dv8tion.jda.api.Permission;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ChannelPermissionOptimizerTest {
    private static final long PUBLIC_ROLE_ID = 1L;
    private final ChannelPermissionOptimizer optimizer = new ChannelPermissionOptimizer();

    @Test
    void shouldRemoveRedundantMemberAllowOverride() {
        long view = Permission.VIEW_CHANNEL.getRawValue();

        ChannelPermissionOptimizer.OverrideState roleOverride = new ChannelPermissionOptimizer.OverrideState(
                10L,
                ChannelPermissionOptimizer.HolderType.ROLE,
                view,
                0L,
                "Cargo VIP"
        );
        ChannelPermissionOptimizer.OverrideState memberOverride = new ChannelPermissionOptimizer.OverrideState(
                100L,
                ChannelPermissionOptimizer.HolderType.MEMBER,
                view,
                0L,
                "Membro Leo"
        );

        ChannelPermissionOptimizer.MemberSnapshot member = new ChannelPermissionOptimizer.MemberSnapshot(
                100L,
                Set.of(10L),
                false,
                0L,
                view,
                true
        );

        var request = new ChannelPermissionOptimizer.OptimizationRequest(
                PUBLIC_ROLE_ID,
                false,
                List.of(roleOverride, memberOverride),
                List.of(member)
        );

        ChannelPermissionOptimizer.AnalysisResult result = ChannelPermissionOptimizer.analyze(request);

        assertTrue(result.validCurrentState());
        assertTrue(result.hasChanges());
        assertEquals(1, result.changedOverrideCount());
        assertTrue(result.changes().getFirst().deletesOverride());
        assertEquals(view, result.changes().getFirst().removedAllowedRaw());
    }

    @Test
    void shouldKeepOverrideWhenRemovingItChangesAccess() {
        long view = Permission.VIEW_CHANNEL.getRawValue();

        ChannelPermissionOptimizer.OverrideState memberOverride = new ChannelPermissionOptimizer.OverrideState(
                100L,
                ChannelPermissionOptimizer.HolderType.MEMBER,
                0L,
                view,
                "Membro Leo"
        );

        ChannelPermissionOptimizer.MemberSnapshot member = new ChannelPermissionOptimizer.MemberSnapshot(
                100L,
                Set.of(),
                false,
                view,
                0L,
                false
        );

        var request = new ChannelPermissionOptimizer.OptimizationRequest(
                PUBLIC_ROLE_ID,
                false,
                List.of(memberOverride),
                List.of(member)
        );

        ChannelPermissionOptimizer.AnalysisResult result = ChannelPermissionOptimizer.analyze(request);

        assertTrue(result.validCurrentState());
        assertFalse(result.hasChanges());
    }

    @Test
    void shouldRejectPlanWhenCurrentSimulationDoesNotMatchExpectedState() {
        long view = Permission.VIEW_CHANNEL.getRawValue();

        ChannelPermissionOptimizer.OverrideState roleOverride = new ChannelPermissionOptimizer.OverrideState(
                10L,
                ChannelPermissionOptimizer.HolderType.ROLE,
                view,
                0L,
                "Cargo VIP"
        );

        ChannelPermissionOptimizer.MemberSnapshot member = new ChannelPermissionOptimizer.MemberSnapshot(
                100L,
                Set.of(10L),
                false,
                0L,
                0L,
                false
        );

        var request = new ChannelPermissionOptimizer.OptimizationRequest(
                PUBLIC_ROLE_ID,
                false,
                List.of(roleOverride),
                List.of(member)
        );

        ChannelPermissionOptimizer.AnalysisResult result = ChannelPermissionOptimizer.analyze(request);

        assertFalse(result.validCurrentState());
        assertNotNull(result.failureReason());
        assertTrue(result.failureReason().contains("JDA"));
    }
}
