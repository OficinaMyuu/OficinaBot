package ofc.bot.internal;

import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BotDataTest {
    @Test
    void configLookupQuotesReservedKeyColumn() {
        String sql = BotData.selectConfigValue(DSL.using(SQLDialect.MYSQL), "app.token")
                .getSQL(ParamType.INLINED);

        assertEquals("select `value` from `config` where `key` = 'app.token'", sql);
    }
}
