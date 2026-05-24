package ofc.bot.domain.entity;

import ofc.bot.domain.tables.RegistersTable;
import ofc.bot.handlers.registration.RegistrationDevice;
import ofc.bot.handlers.registration.RegistrationGender;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;

public class RegisterData extends OficinaRecord<RegisterData> {
    private static final RegistersTable REGISTERS = RegistersTable.REGISTERS;

    public RegisterData() {
        super(REGISTERS);
    }

    public RegisterData(
            @NotNull RegistrationGender gender,
            @NotNull RegistrationDevice device,
            int age,
            long targetId,
            long moderatorId,
            long createdAt
    ) {
        this();
        set(REGISTERS.TARGET_ID, targetId);
        set(REGISTERS.MODERATOR_ID, moderatorId);
        set(REGISTERS.GENDER, gender.name());
        set(REGISTERS.DEVICE, device.name());
        set(REGISTERS.AGE, age);
        set(REGISTERS.CREATED_AT, createdAt);
    }

    public RegisterData(
            @NotNull RegistrationGender gender,
            @NotNull RegistrationDevice device,
            int age,
            long targetId,
            long moderatorId
    ) {
        this(gender, device, age, targetId, moderatorId, Bot.unixNow());
    }

    public int getId() {
        return get(REGISTERS.ID);
    }

    public long getTargetId() {
        return get(REGISTERS.TARGET_ID);
    }

    public long getModeratorId() {
        return get(REGISTERS.MODERATOR_ID);
    }

    public int getAge() {
        return get(REGISTERS.AGE);
    }

    @NotNull
    public RegistrationGender getGender() {
        return RegistrationGender.valueOf(get(REGISTERS.GENDER));
    }

    @NotNull
    public RegistrationDevice getDevice() {
        return RegistrationDevice.valueOf(get(REGISTERS.DEVICE));
    }

    public long getTimeCreated() {
        return get(REGISTERS.CREATED_AT);
    }
}
