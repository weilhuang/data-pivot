package com.data.pivot.plugin.view;

import com.data.pivot.plugin.entity.DatabaseQueryConfig;
import com.data.pivot.plugin.enums.DBType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UiTestFixtures {
    private UiTestFixtures() {
    }

    public static DatabaseQueryConfig userAccountConfig() {
        return new DatabaseQueryConfig(
                "ds",
                DBType.MYSQL,
                "jdbc:mysql://localhost:3306/demo",
                "user",
                "password",
                "demo",
                "",
                "user_account",
                List.of("id", "name"),
                "name"
        );
    }

    public static List<Map<String, Object>> userAccountRows() {
        Map<String, Object> alice = new LinkedHashMap<>();
        alice.put("id", 1);
        alice.put("name", "alice");
        Map<String, Object> bob = new LinkedHashMap<>();
        bob.put("id", 2);
        bob.put("name", "bob");
        return List.of(alice, bob);
    }

    public static List<Map<String, Object>> analysisRows() {
        Map<String, Object> active = new LinkedHashMap<>();
        active.put("name", "ACTIVE");
        active.put("rs_count", 18);
        active.put("percentage", 90.0);
        Map<String, Object> disabled = new LinkedHashMap<>();
        disabled.put("name", "DISABLED");
        disabled.put("rs_count", 2);
        disabled.put("percentage", 10.0);
        return List.of(active, disabled);
    }
}
