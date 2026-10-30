package com.ordercraft.service.impl;

import com.ordercraft.dto.request.SupplierRequest;
import com.ordercraft.dto.response.SupplierResponse;
import com.ordercraft.entity.Supplier;
import com.ordercraft.exception.BusinessRuleException;
import com.ordercraft.exception.ResourceNotFoundException;
import com.ordercraft.repository.SupplierRepository;
import com.ordercraft.service.SupplierService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierServiceImpl(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponse> getAllSuppliers(String query, Pageable pageable) {
        Page<Supplier> suppliers;
        if (StringUtils.hasText(query)) {
            suppliers = supplierRepository.searchSuppliers(query.trim(), pageable);
        } else {
            suppliers = supplierRepository.findByActiveTrue(pageable);
        }
        return suppliers.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
        return mapToResponse(supplier);
    }

    @Override
    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request) {
        String code = request.getSupplierCode();
        if (!StringUtils.hasText(code)) {
            code = "SUP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } else if (supplierRepository.existsBySupplierCode(code)) {
            throw new BusinessRuleException("Supplier with code '" + code + "' already exists");
        }

        if (supplierRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Supplier with email '" + request.getEmail() + "' already exists");
        }

        Supplier supplier = new Supplier(
                code,
                request.getCompanyName(),
                request.getContactPerson(),
                request.getEmail(),
                request.getPhone(),
                request.getAddress(),
                request.getPaymentTerms()
        );
        supplier.setTaxId(request.getTaxId());

        Supplier saved = supplierRepository.save(supplier);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));

        supplier.setCompanyName(request.getCompanyName());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        supplier.setTaxId(request.getTaxId());
        supplier.setPaymentTerms(request.getPaymentTerms());

        Supplier updated = supplierRepository.save(supplier);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
        supplier.setActive(false);
        supplierRepository.save(supplier);
    }

    private SupplierResponse mapToResponse(Supplier supplier) {
        SupplierResponse res = new SupplierResponse();
        res.setId(supplier.getId());
        res.setSupplierCode(supplier.getSupplierCode());
        res.setCompanyName(supplier.getCompanyName());
        res.setContactPerson(supplier.getContactPerson());
        res.setEmail(supplier.getEmail());
        res.setPhone(supplier.getPhone());
        res.setAddress(supplier.getAddress());
        res.setTaxId(supplier.getTaxId());
        res.setPaymentTerms(supplier.getPaymentTerms());
        res.setActive(supplier.isActive());
        res.setCreatedAt(supplier.getCreatedAt());
        return res;
    }
}
