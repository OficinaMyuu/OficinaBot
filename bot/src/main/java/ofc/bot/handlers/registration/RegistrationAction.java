package ofc.bot.handlers.registration;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record RegistrationAction(
        @NotNull RegistrationGender gender,
        @NotNull RegistrationDevice device,
        int age
) {
    public boolean hasValidAge() {
        return this.age > 0;
    }

    @NotNull
    public List<RegistrationRole> rolesToAdd() {
        List<RegistrationRole> roles = new ArrayList<>(5);
        roles.add(RegistrationRole.REGISTERED);
        roles.add(this.device.role());
        roles.add(this.gender.role());
        roles.addAll(RegistrationRole.getByAge(this.age));
        return List.copyOf(roles);
    }

    @NotNull
    public List<RegistrationRole> rolesToRemove() {
        return List.of(RegistrationRole.NON_REGISTERED);
    }
}
