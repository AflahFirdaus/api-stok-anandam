package com.stok.anandam.store;

import com.stok.anandam.store.core.postgres.model.IjinImport;
import com.stok.anandam.store.core.postgres.repository.IjinImportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@SpringBootTest
public class IjinImportTest {

    @Autowired
    private IjinImportRepository ijinImportRepository;

    @Test
    public void testQuery() {
        try {
            System.out.println("==================================================");
            System.out.println("TESTING IJIN IMPORT REPOSITORY...");
            Page<IjinImport> page = ijinImportRepository.findByFilters("", PageRequest.of(0, 10));
            System.out.println("TOTAL ELEMENTS: " + page.getTotalElements());
            for (IjinImport item : page.getContent()) {
                System.out.println("ITEM: " + item.getNamaBarang());
            }
            System.out.println("==================================================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
