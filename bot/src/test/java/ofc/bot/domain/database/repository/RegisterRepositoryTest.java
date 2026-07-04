package ofc.bot.domain.database.repository;

import ofc.bot.testing.MySQLTestDatabase;

import ofc.bot.domain.entity.RegisterData;
import ofc.bot.domain.tables.RegistersTable;
import ofc.bot.handlers.registration.RegistrationDevice;
import ofc.bot.handlers.registration.RegistrationGender;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRepositoryTest {
    @Test
    void shouldSaveAndFindRegisterDataById() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            DSLContext ctx = MySQLTestDatabase.context(connection);
            RegistersTable.REGISTERS.getSchema(ctx).execute();
            RegisterRepository repository = new RegisterRepository(ctx);
            RegisterData entry = new RegisterData(
                    RegistrationGender.FEMALE,
                    RegistrationDevice.DESKTOP,
                    22,
                    123L,
                    456L,
                    789L
            );

            repository.save(entry);

            RegisterData saved = repository.findById(1);
            assertNotNull(saved);
            assertEquals(1, saved.getId());
            assertEquals(123L, saved.getTargetId());
            assertEquals(456L, saved.getModeratorId());
            assertEquals(22, saved.getAge());
            assertEquals(RegistrationGender.FEMALE, saved.getGender());
            assertEquals(RegistrationDevice.DESKTOP, saved.getDevice());
            assertEquals(789L, saved.getTimeCreated());
        }
    }
}
