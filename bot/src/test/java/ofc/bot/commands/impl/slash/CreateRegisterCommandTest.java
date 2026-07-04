package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.database.repository.RegisterRepository;
import ofc.bot.handlers.registration.RegistrationDevice;
import ofc.bot.handlers.registration.RegistrationGender;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreateRegisterCommandTest {
    @Test
    void shouldUseEnumNamesAsChoiceValues() {
        CreateRegisterCommand command = new CreateRegisterCommand((RegisterRepository) null);
        List<OptionData> options = command.getOptions();
        OptionData gender = option(options, "gender");
        OptionData device = option(options, "device");

        assertChoice(gender, "Female", RegistrationGender.FEMALE.name());
        assertChoice(gender, "Male", RegistrationGender.MALE.name());
        assertChoice(gender, "Non-Binary", RegistrationGender.NON_BINARY.name());
        assertChoice(device, "Mobile", RegistrationDevice.MOBILE.name());
        assertChoice(device, "PC", RegistrationDevice.DESKTOP.name());
    }

    private OptionData option(List<OptionData> options, String name) {
        return options.stream()
                .filter(option -> option.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private void assertChoice(OptionData option, String name, String value) {
        Command.Choice choice = option.getChoices()
                .stream()
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow();

        assertEquals(value, choice.getAsString());
    }
}
