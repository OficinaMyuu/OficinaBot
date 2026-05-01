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
}
