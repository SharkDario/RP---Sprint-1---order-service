package com.mindhub.order_service.repositories;


import com.mindhub.order_service.models.EntityOrder;
import com.mindhub.order_service.models.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

// This annotation is used for JPA tests, it configures an in-memory database and JPA repositories
@DataJpaTest
public class OrderRepositoryTest {
    @Autowired
    private OrderRepository orderRepository;

    private EntityOrder order;

    @BeforeEach
    public void setUp(){
        // Create an order
        order = new EntityOrder();
        order.setUserId(1L);
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);
    }

    @Test
    public void testCreateNewOrder() {
        EntityOrder newOrder = new EntityOrder();
        newOrder.setUserId(2L);
        newOrder.setStatus(OrderStatus.COMPLETED);
        EntityOrder orderToSave = orderRepository.save(newOrder);
        assertNotNull(orderToSave.getId());
        assertEquals(2L, orderToSave.getUserId());
    }

    @Test
    public void testUpdateOrder() {
        EntityOrder foundOrder = orderRepository.findById(order.getId()).orElse(null);
        assertThat(foundOrder).isNotNull();

        foundOrder.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(foundOrder);

        EntityOrder updateOrder = orderRepository.findById(order.getId()).orElse(null);
        assertThat(updateOrder).isNotNull();
        assertThat(updateOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    public void testDeleteOrder() {
        orderRepository.deleteById(order.getId());
        boolean exists = orderRepository.existsById(order.getId());
        assertFalse(exists);
    }
}
