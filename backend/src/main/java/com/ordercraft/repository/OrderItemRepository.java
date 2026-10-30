package com.ordercraft.repository;

import com.ordercraft.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByCustomerOrderId(Long customerOrderId);
    List<OrderItem> findByProductId(Long productId);
}
