package com.stok.anandam.store;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class CheckMarketingDataTest {

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Test
    void checkEmpCodes() {
        System.out.println("=== ALL DISTINCT emp_code IN old_sales ===");
        try {
            List<Map<String, Object>> allOld = jdbcTemplate.queryForList(
                "SELECT DISTINCT emp_code, emp_name, COUNT(*) as cnt, MIN(doc_date) as min_date, MAX(doc_date) as max_date " +
                "FROM old_sales GROUP BY emp_code, emp_name ORDER BY emp_code"
            );
            for (Map<String, Object> row : allOld) {
                System.out.println("ALL OLD_SALES -> code: [" + row.get("emp_code") + "], name: [" + row.get("emp_name") + 
                                   "], cnt: " + row.get("cnt") + ", min: " + row.get("min_date") + ", max: " + row.get("max_date"));
            }
        } catch (Exception e) {
            System.err.println("Error querying all old_sales: " + e.getMessage());
        }

        System.out.println("=== ALL DISTINCT emp_code IN sales ===");
        try {
            List<Map<String, Object>> sales = jdbcTemplate.queryForList(
                "SELECT DISTINCT emp_code, emp_name, COUNT(*) as cnt, MIN(doc_date) as min_date, MAX(doc_date) as max_date " +
                "FROM sales GROUP BY emp_code, emp_name ORDER BY emp_code"
            );
            for (Map<String, Object> row : sales) {
                System.out.println("SALES -> code: [" + row.get("emp_code") + "], name: [" + row.get("emp_name") + 
                                   "], cnt: " + row.get("cnt") + ", min: " + row.get("min_date") + ", max: " + row.get("max_date"));
            }
        } catch (Exception e) {
            System.err.println("Error querying sales: " + e.getMessage());
        }
    }
}
