package ofc.bot.util.content;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StaffTest {

    @Test
    void shouldFindRoleInsideRequestedScope() {
        boolean result = Staff.hasRoleIdInScope(List.of(Staff.EVENTS_MAIN.getId()), Staff.Scope.EVENTS);

        assertTrue(result);
    }

    @Test
    void shouldIgnoreRolesFromDifferentScopes() {
        boolean result = Staff.hasRoleIdInScope(List.of(Staff.MOV_CALL_MAIN.getId()), Staff.Scope.EVENTS);

        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenMemberHasNoScopedRoles() {
        boolean result = Staff.hasRoleIdInScope(List.of(), Staff.Scope.EVENTS);

        assertFalse(result);
    }

    @Test
    void shouldAllowSupportSuperiorOrHigherRole() {
        assertTrue(Staff.hasRoleIdInScopeAtLeast(
                List.of(Staff.AJUDANTES_SUPERIOR.getId()),
                Staff.Scope.SUPPORT,
                Staff.AJUDANTES_SUPERIOR.getSeniority()
        ));
        assertTrue(Staff.hasRoleIdInScopeAtLeast(
                List.of(Staff.AJUDANTES_VICE_LEADER.getId()),
                Staff.Scope.SUPPORT,
                Staff.AJUDANTES_SUPERIOR.getSeniority()
        ));
        assertTrue(Staff.hasRoleIdInScopeAtLeast(
                List.of(Staff.AJUDANTES_CO_LEADER.getId()),
                Staff.Scope.SUPPORT,
                Staff.AJUDANTES_SUPERIOR.getSeniority()
        ));
    }

    @Test
    void shouldRejectSupportRolesBelowSuperior() {
        assertFalse(Staff.hasRoleIdInScopeAtLeast(
                List.of(Staff.AJUDANTES_MAIN.getId()),
                Staff.Scope.SUPPORT,
                Staff.AJUDANTES_SUPERIOR.getSeniority()
        ));
        assertFalse(Staff.hasRoleIdInScopeAtLeast(
                List.of(Staff.AJUDANTES_TRAINEE.getId()),
                Staff.Scope.SUPPORT,
                Staff.AJUDANTES_SUPERIOR.getSeniority()
        ));
    }

    @Test
    void shouldRejectNonSupportSuperiorRoles() {
        assertFalse(Staff.hasRoleIdInScopeAtLeast(
                List.of(Staff.EVENTS_SUPERIOR.getId()),
                Staff.Scope.SUPPORT,
                Staff.AJUDANTES_SUPERIOR.getSeniority()
        ));
        assertFalse(Staff.hasRoleIdInScopeAtLeast(
                List.of(Staff.MOV_CALL_SUPERIOR.getId()),
                Staff.Scope.SUPPORT,
                Staff.AJUDANTES_SUPERIOR.getSeniority()
        ));
        assertFalse(Staff.hasRoleIdInScopeAtLeast(
                List.of(Staff.GENERAL.getId()),
                Staff.Scope.SUPPORT,
                Staff.AJUDANTES_SUPERIOR.getSeniority()
        ));
    }
}
