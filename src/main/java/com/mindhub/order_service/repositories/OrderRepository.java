package com.mindhub.order_service.repositories;

import com.mindhub.order_service.models.EntityOrder;
import com.mindhub.order_service.models.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<EntityOrder, Long> {
    List<EntityOrder> findByStatus(OrderStatus status);
    List<EntityOrder> findByUserId(long userId);
    List<EntityOrder> findByUserIdAndStatus(long userId, OrderStatus status);

    boolean existsByUserId(long userId);
    boolean existsById(long id);

    int countById(long id);
    int countByStatus(OrderStatus status);
    int countByUserId(long userId);

    // Pagination example
    Page<EntityOrder> findByStatus(OrderStatus status, Pageable pageable);
    Page<EntityOrder> findByUserId(long userId, Pageable pageable);
}
