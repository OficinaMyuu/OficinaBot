package ofc.bot.handlers.registration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationActionTest {
    @Test
    void shouldSelectAdultRoleForAdults() {
        RegistrationAction action = new RegistrationAction(RegistrationGender.FEMALE, RegistrationDevice.DESKTOP, 18);

        assertEquals(List.of(
                RegistrationRole.REGISTERED,
                RegistrationRole.DESKTOP,
                RegistrationRole.FEMALE,
                RegistrationRole.ADULT
        ), action.rolesToAdd());
    }

    @Test
    void shouldSelectUnderageRoleForMinors() {
        RegistrationAction action = new RegistrationAction(RegistrationGender.MALE, RegistrationDevice.MOBILE, 16);

        assertEquals(List.of(
                RegistrationRole.REGISTERED,
                RegistrationRole.MOBILE,
                RegistrationRole.MALE,
                RegistrationRole.UNDERAGE
        ), action.rolesToAdd());
    }

    @Test
    void shouldSelectTweenAndUnderageRolesForChildrenUnderThirteen() {
        RegistrationAction action = new RegistrationAction(RegistrationGender.NON_BINARY, RegistrationDevice.DESKTOP, 12);

        assertEquals(List.of(
                RegistrationRole.REGISTERED,
                RegistrationRole.DESKTOP,
                RegistrationRole.NON_BINARY,
                RegistrationRole.UNDERAGE,
                RegistrationRole.TWEEN
        ), action.rolesToAdd());
    }

    @Test
    void shouldRemoveOnlyNonRegisteredRole() {
        RegistrationAction action = new RegistrationAction(RegistrationGender.FEMALE, RegistrationDevice.MOBILE, 20);

        assertEquals(List.of(RegistrationRole.NON_REGISTERED), action.rolesToRemove());
    }

    @Test
    void shouldRejectNonPositiveAges() {
        assertFalse(new RegistrationAction(RegistrationGender.FEMALE, RegistrationDevice.MOBILE, 0).hasValidAge());
        assertFalse(new RegistrationAction(RegistrationGender.FEMALE, RegistrationDevice.MOBILE, -1).hasValidAge());
        assertTrue(new RegistrationAction(RegistrationGender.FEMALE, RegistrationDevice.MOBILE, 1).hasValidAge());
    }
}
