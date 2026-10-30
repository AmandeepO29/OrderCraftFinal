package com.ordercraft.service;

import com.ordercraft.dto.request.BomItemRequest;
import com.ordercraft.dto.request.BomRequest;
import com.ordercraft.dto.response.BomAvailabilityCheckResponse;
import com.ordercraft.dto.response.BomResponse;
import com.ordercraft.entity.*;
import com.ordercraft.repository.BomRepository;
import com.ordercraft.repository.InventoryRepository;
import com.ordercraft.repository.ProductRepository;
import com.ordercraft.repository.RawMaterialRepository;
import com.ordercraft.service.impl.BomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BomServiceTest {

    @Mock
    private BomRepository bomRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RawMaterialRepository rawMaterialRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private BomServiceImpl bomService;

    private Product product;
    private RawMaterial rawMaterial;
    private Bom bom;

    @BeforeEach
    void setUp() {
        product = new Product("SKU-100", "Industrial Actuator", "Robotic arm part", "Robotics", new BigDecimal("500.00"), 5);
        product.setId(1L);

        rawMaterial = new RawMaterial("RM-ALUM", "Aluminum Bar", "Grade 6061", "kg", new BigDecimal("10.00"), new BigDecimal("50.00"), new BigDecimal("200.00"));
        rawMaterial.setId(10L);

        bom = new Bom(product, "BOM-SKU-100", "1.0", "Assembly formulation");
        bom.setId(100L);
        bom.addItem(new BomItem(rawMaterial, new BigDecimal("5.0"), "kg", BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Should create BOM and calculate estimated cost accurately")
    void testCreateBom() {
        BomRequest request = new BomRequest();
        request.setProductId(1L);
        request.setBomCode("BOM-SKU-100");
        request.setVersion("1.0");

        BomItemRequest itemReq = new BomItemRequest();
        itemReq.setRawMaterialId(10L);
        itemReq.setQuantityRequired(new BigDecimal("5.0"));
        request.setItems(List.of(itemReq));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(bomRepository.existsByProductId(1L)).thenReturn(false);
        when(bomRepository.existsByBomCode("BOM-SKU-100")).thenReturn(false);
        when(rawMaterialRepository.findById(10L)).thenReturn(Optional.of(rawMaterial));
        when(bomRepository.save(any(Bom.class))).thenReturn(bom);

        BomResponse response = bomService.createBom(request);

        assertNotNull(response);
        assertEquals("BOM-SKU-100", response.getBomCode());
        assertEquals(new BigDecimal("50.00"), bom.getTotalEstimatedCost());
        verify(bomRepository, times(1)).save(any(Bom.class));
    }

    @Test
    @DisplayName("Should validate material availability successfully when inventory stock is sufficient")
    void testCheckMaterialAvailability_Sufficient() {
        Inventory inventory = Inventory.ofRawMaterial(rawMaterial, new BigDecimal("100.00"), "Bay 1");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(bomRepository.findByProductId(1L)).thenReturn(Optional.of(bom));
        when(inventoryRepository.findByRawMaterialId(10L)).thenReturn(Optional.of(inventory));

        BomAvailabilityCheckResponse response = bomService.checkMaterialAvailability(1L, 10);

        assertNotNull(response);
        assertTrue(response.isSufficient());
        assertEquals(1, response.getRequirements().size());
        assertTrue(response.getRequirements().get(0).isAvailable());
    }

    @Test
    @DisplayName("Should flag insufficient availability when inventory stock is inadequate")
    void testCheckMaterialAvailability_Insufficient() {
        Inventory inventory = Inventory.ofRawMaterial(rawMaterial, new BigDecimal("10.00"), "Bay 1");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(bomRepository.findByProductId(1L)).thenReturn(Optional.of(bom));
        when(inventoryRepository.findByRawMaterialId(10L)).thenReturn(Optional.of(inventory));

        BomAvailabilityCheckResponse response = bomService.checkMaterialAvailability(1L, 10); // requires 50 kg

        assertNotNull(response);
        assertFalse(response.isSufficient());
        assertFalse(response.getRequirements().get(0).isAvailable());
    }
}
