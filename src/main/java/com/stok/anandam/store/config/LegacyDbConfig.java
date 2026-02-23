package com.stok.anandam.store.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "app.mysql.enabled", havingValue = "true")
public class LegacyDbConfig {

    @Bean(name = "legacyJdbcTemplate")
    public JdbcTemplate legacyJdbcTemplate(@Qualifier("mysqlDataSource") DataSource ds) {
        JdbcTemplate template = new JdbcTemplate(ds);
        // PENTING: Agar MySQL mau streaming data row-per-row, bukan load semua ke RAM
        template.setFetchSize(Integer.MIN_VALUE);
        return template;
    }
}