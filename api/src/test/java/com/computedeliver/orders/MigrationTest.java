package com.computedeliver.orders;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MigrationTest extends PostgresTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migrationsApplyOnFreshDatabase() {
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'", String.class);
        assertThat(tables).contains("clients", "products", "orders", "order_lines", "order_status_history");
        Integer version = jdbc.queryForObject(
                "select count(*) from information_schema.columns where table_name = 'orders' and column_name = 'version'",
                Integer.class);
        assertThat(version).isEqualTo(1);
    }
}
