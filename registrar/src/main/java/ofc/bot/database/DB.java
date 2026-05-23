package ofc.bot.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static ofc.bot.database.models.RegisterData.Table.REGISTER_DATA;

public class DB {
    private static final Logger LOGGER = LoggerFactory.getLogger(DB.class);
    private static final File DATABASE_FILE = new File("database.db");
    private static HikariDataSource dataSource;

    private DB() {}

    public static DSLContext context() {
        return DSL.using(dataSource, SQLDialect.SQLITE);
    }

    public static void init() {

        LOGGER.info("Initializing database...");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + DATABASE_FILE);

        dataSource = new HikariDataSource(config);
        DatabaseInitializer.init();
    }

    private static final class DatabaseInitializer {
        private DatabaseInitializer() {}

        static void init() {

            DSLContext ctx = context();

            ctx.createTableIfNotExists(REGISTER_DATA)
                    .primaryKey(REGISTER_DATA.ID)
                    .columns(REGISTER_DATA.fields())
                    .execute();

            LOGGER.info("Table \"{}\" successfully created", REGISTER_DATA.getName());
        }
    }
}