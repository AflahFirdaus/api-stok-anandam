package com.stok.anandam.store.config; // Sesuaikan package

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.stok.anandam.store.core.postgres.repository", entityManagerFactoryRef = "pgEntityManagerFactory", transactionManagerRef = "pgTransactionManager")
public class PostgresConfig {

    @Value("${spring.datasource.pg.jdbc-url}")
    private String url;

    @Value("${spring.datasource.pg.username}")
    private String username;

    @Value("${spring.datasource.pg.password}")
    private String password;

    @Value("${spring.datasource.pg.driver-class-name}")
    private String driverClassName;

    @Primary
    @Bean(name = "pgDataSource")
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .type(com.zaxxer.hikari.HikariDataSource.class)
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    @Primary
    @Bean(name = "pgEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("pgDataSource") DataSource dataSource) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        // === TAMBAHKAN 3 BARIS INI BIAR NGEBUT ===
        properties.put("hibernate.jdbc.batch_size", "50"); // Tumpuk 50 data per insert
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        return builder
                .dataSource(dataSource)
                .packages("com.stok.anandam.store.core.postgres.model")
                .persistenceUnit("postgres")
                .properties(properties)
                .build();
    }

    @Primary
    @Bean(name = "pgTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("pgEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory.getObject()));
    }

    @Bean(name = "pgJdbcTemplate")
    public org.springframework.jdbc.core.JdbcTemplate pgJdbcTemplate(@Qualifier("pgDataSource") DataSource dataSource) {
        return new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    }
}