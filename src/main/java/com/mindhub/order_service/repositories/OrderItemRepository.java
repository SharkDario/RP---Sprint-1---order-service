package com.mindhub.order_service.repositories;

import com.mindhub.order_service.models.EntityOrder;
import com.mindhub.order_service.models.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByEntityOrder(EntityOrder entityOrder);
    List<OrderItem> findByEntityOrderId(Long orderId);
    List<OrderItem> findByProductId(long productId);
    List<OrderItem> findByQuantity(Integer quantity);

    boolean existsById(long id);
    boolean existsByEntityOrder(EntityOrder entityOrder);
    boolean existsByProductId(long productId);

    int countById(long id);
    int countByEntityOrder(EntityOrder entityOrder);
    int countByProductId(long productId);

    // Pagination example
    Page<OrderItem> findByEntityOrder(EntityOrder entityOrder, Pageable pageable);
    Page<OrderItem> findByProductId(long productId, Pageable pageable);
}
