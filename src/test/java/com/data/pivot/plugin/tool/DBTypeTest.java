package com.data.pivot.plugin.tool;

import com.data.pivot.plugin.enums.DBType;
import com.data.pivot.plugin.enums.DefaultStrategyType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DBTypeTest {
    @Test
    public void getByNameResolvesCanonicalNamesAndAliases() {
        assertEquals(DBType.MYSQL, DBType.getByName("MySQL"));
        assertEquals(DBType.POSTGRES, DBType.getByName("Postgres"));
        assertEquals(DBType.MSSQL, DBType.getByName("SQL Server"));
        assertEquals(DBType.MSSQL, DBType.getByName("MSSQL"));
        assertEquals(DBType.ORACLE, DBType.getByName("Oracle"));
        assertEquals(DBType.MONGO, DBType.getByName("Mongo"));
        assertNull(DBType.getByName(null));
        assertNull(DBType.getByName("Redis"));
    }

    @Test
    public void jdbcQuerySupportExcludesMongoAndUnknown() {
        assertTrue(DBType.supportsJdbcQuery(DBType.MYSQL));
        assertTrue(DBType.supportsJdbcQuery(DBType.POSTGRES));
        assertTrue(DBType.supportsJdbcQuery(DBType.ORACLE));
        assertTrue(DBType.supportsJdbcQuery(DBType.MSSQL));
        assertFalse(DBType.supportsJdbcQuery(DBType.MONGO));
        assertFalse(DBType.supportsJdbcQuery(null));
    }

    @Test
    public void defaultStrategyCodesStayStable() {
        assertEquals("JPA", DefaultStrategyType.JPAAnnotation.getCode());
        assertEquals("MybatisPlus", DefaultStrategyType.MPAnnotation.getCode());
        assertEquals("HumpUnderline", DefaultStrategyType.HUMP_UNDERLINE.getCode());
    }
}
