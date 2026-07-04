package ofc.bot.internal.data;

import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BotPropertiesTest {
    @Test
    void configLookupQuotesReservedKeyColumn() {
        String sql = BotProperties.selectConfigValue(DSL.using(SQLDialect.MYSQL), "app.token")
                .getSQL(ParamType.INLINED);

        assertEquals("select `value` from `config` where `key` = 'app.token'", sql);
    }
}
