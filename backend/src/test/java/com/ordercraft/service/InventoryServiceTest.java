package com.ordercraft.service;

import com.ordercraft.dto.request.StockAdjustmentRequest;
import com.ordercraft.dto.response.InventoryResponse;
import com.ordercraft.entity.Inventory;
import com.ordercraft.entity.RawMaterial;
import com.ordercraft.exception.InsufficientInventoryException;
import com.ordercraft.repository.InventoryRepository;
import com.ordercraft.repository.InventoryTransactionRepository;
import com.ordercraft.repository.UserRepository;
import com.ordercraft.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryTransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Inventory inventory;
    private RawMaterial rawMaterial;

    @BeforeEach
    void setUp() {
        rawMaterial = new RawMaterial("RM-01", "Sheet Metal", "304 SS", "sq_m", new BigDecimal("20.00"), new BigDecimal("10.00"), new BigDecimal("50.00"));
        inventory = Inventory.ofRawMaterial(rawMaterial, new BigDecimal("50.00"), "Bay A");
        inventory.setId(1L);
    }

    @Test
    @DisplayName("Should increment inventory stock when adjustment is ADD")
    void testAdjustStock_Add() {
        StockAdjustmentRequest request = new StockAdjustmentRequest();
        request.setInventoryId(1L);
        request.setType("ADD");
        request.setQuantity(new BigDecimal("25.00"));
        request.setNotes("Audit surplus adjustment");

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        InventoryResponse response = inventoryService.adjustStock(request, "admin");

        assertNotNull(response);
        assertEquals(new BigDecimal("75.00"), inventory.getCurrentStock());
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should prevent negative inventory and throw InsufficientInventoryException when deduction exceeds stock")
    void testAdjustStock_PreventNegativeInventory() {
        StockAdjustmentRequest request = new StockAdjustmentRequest();
        request.setInventoryId(1L);
        request.setType("DEDUCT");
        request.setQuantity(new BigDecimal("100.00")); // current is 50
        request.setNotes("Excess deduction test");

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));

        assertThrows(InsufficientInventoryException.class, () -> {
            inventoryService.adjustStock(request, "admin");
        });
        verify(inventoryRepository, never()).save(any());
    }
}
