package ofc.bot.handlers.nick;

import net.dv8tion.jda.api.entities.Member;
import ofc.bot.util.content.Staff;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NicknameTargetPolicy {

    public TargetValidation validate(@NotNull Member issuer, @NotNull Member target) {
        if (target.getUser().isBot()) {
            return TargetValidation.reject("Bots n\u00e3o podem receber pedidos de altera\u00e7\u00e3o de apelido.");
        }

        if (Staff.isStaff(target)) {
            return TargetValidation.reject("Staffs n\u00e3o podem solicitar altera\u00e7\u00e3o de apelido para outros staffs.");
        }

        if (!issuer.canInteract(target)) {
            return TargetValidation.reject("Voc\u00ea n\u00e3o pode solicitar altera\u00e7\u00e3o de apelido para esse membro pela hierarquia atual de cargos.");
        }

        return TargetValidation.accept();
    }

    public record TargetValidation(boolean accepted, @Nullable String rejectionReason) {
        private static TargetValidation accept() {
            return new TargetValidation(true, null);
        }

        private static TargetValidation reject(String reason) {
            return new TargetValidation(false, reason);
        }
    }
}
