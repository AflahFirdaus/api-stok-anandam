package com.stok.anandam.store.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@ConditionalOnProperty(name = "app.mysql.enabled", havingValue = "true")
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.stok.anandam.store.core.mysql.repository", entityManagerFactoryRef = "mysqlEntityManagerFactory", transactionManagerRef = "mysqlTransactionManager")
public class MysqlConfig {

    private static final Logger log = LoggerFactory.getLogger(MysqlConfig.class);

    @Value("${spring.datasource.mysql.jdbc-url}")
    private String url;

    @Value("${spring.datasource.mysql.username}")
    private String username;

    @Value("${spring.datasource.mysql.password}")
    private String password;

    @Value("${spring.datasource.mysql.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.mysql.maximum-pool-size:5}")
    private int maximumPoolSize;

    @Value("${spring.datasource.mysql.minimum-idle:2}")
    private int minimumIdle;

    @Value("${spring.datasource.mysql.connection-timeout:60000}")
    private long connectionTimeout;

    @Value("${spring.datasource.mysql.idle-timeout:30000}")
    private long idleTimeout;

    @Bean(name = "mysqlDataSource")
    public DataSource mysqlDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driverClassName);

        // Pool Config
        ds.setMaximumPoolSize(maximumPoolSize);
        ds.setMinimumIdle(minimumIdle);
        ds.setConnectionTimeout(connectionTimeout);
        ds.setIdleTimeout(idleTimeout);
        ds.setPoolName("HikariPool-MySQL");
        // Jangan gagal startup jika MySQL belum siap; error akan terlihat saat migrasi + logConnectionCause
        ds.setInitializationFailTimeout(-1);

        // Test koneksi sekali dan log hasil (tanpa gagalkan startup)
        try (Connection c = ds.getConnection()) {
            log.info("MySQL connection OK: {} (driver: {})", url.replaceFirst("jdbc:mysql://[^/]+", "jdbc:mysql://***"), driverClassName);
        } catch (Exception e) {
            log.warn("MySQL connection check failed (migrasi akan gagal sampai MySQL bisa diakses): {} - {}", e.getClass().getSimpleName(), e.getMessage());
            if (e.getCause() != null) log.warn("  Cause: {}", e.getCause().getMessage());
        }

        return ds;
    }

    @Bean(name = "mysqlJdbcTemplate")
    public JdbcTemplate mysqlJdbcTemplate(@Qualifier("mysqlDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "mysqlEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("mysqlDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.hbm2ddl.auto", "none");

        return builder
                .dataSource(dataSource)
                .packages("com.stok.anandam.store.core.mysql.model")
                .persistenceUnit("mysql")
                .properties(properties)
                .build();
    }

    @Bean(name = "mysqlTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("mysqlEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory.getObject()));
    }
}