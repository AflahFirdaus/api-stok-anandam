package com.stok.anandam.store.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

// DINONAKTIFKAN PERMANEN: Migrasi MyBiz telah dipindah ke project api-migration.
@Profile("mysql-disabled")
public class LegacyDbConfig {

    @Bean(name = "legacyJdbcTemplate")
    public JdbcTemplate legacyJdbcTemplate(@Qualifier("mysqlDataSource") DataSource ds) {
        JdbcTemplate template = new JdbcTemplate(ds);
        // PENTING: Agar MySQL mau streaming data row-per-row, bukan load semua ke RAM
        template.setFetchSize(Integer.MIN_VALUE);
        return template;
    }
}