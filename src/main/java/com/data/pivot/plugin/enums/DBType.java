package com.data.pivot.plugin.enums;

import java.util.Arrays;

public enum DBType {
    MYSQL("MySQL"),
    POSTGRES("PostgreSQL", "Postgres"),
    ORACLE("Oracle"),
    MSSQL("SQL Server", "Microsoft SQL Server", "MSSQL"),
    MONGO("MongoDB", "Mongo"),
    ;
    private final String name;
    private final String[] aliases;

    public String getName() {
        return name;
    }

    public static DBType getByName(String name) {
        if (name == null) {
            return null;
        }
        for (DBType value : values()) {
            if (value.getName().equalsIgnoreCase(name)
                    || Arrays.stream(value.aliases).anyMatch(alias -> alias.equalsIgnoreCase(name))) {
                return value;
            }
        }
        return null;
    }

    public static boolean supportsJdbcQuery(DBType type) {
        return type == MYSQL || type == POSTGRES || type == ORACLE || type == MSSQL;
    }

    DBType(String name, String... aliases) {
        this.name = name;
        this.aliases = aliases;
    }
}
