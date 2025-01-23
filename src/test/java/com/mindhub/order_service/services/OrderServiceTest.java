package com.mindhub.order_service.services;

import com.mindhub.order_service.models.EntityOrder;
import com.mindhub.order_service.models.OrderStatus;
import com.mindhub.order_service.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Use @SpringBootTest to load the full Spring context and verify component integration
@SpringBootTest
@ActiveProfiles("test")
public class OrderServiceTest {
    // Allows for creating and managing mocks of dependencies in unit tests,
    // facilitating the simulation of external components.
    // Simulacrum
    @MockBean
    private OrderRepository orderRepository;
    // Use @MockBean to replace real beans with mocks during testing, allowing you to focus on specific interactions.
    @Autowired
    private OrderService orderService;

    private EntityOrder testOrder;

    @BeforeEach
    public void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        testOrder = spy(new EntityOrder(OrderStatus.PENDING));
        testOrder.setUserId(1L);
        when(testOrder.getId()).thenReturn(1L);
        // Mock the repository to return the test user when findByEmail is called
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        when(orderRepository.existsById(testOrder.getId())).thenReturn(true);
    }

    @Test
    public void testGetOrderById() {
        // Mock the repository to return the test user when findById is called
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Call the service method
        EntityOrder result = orderService.getEntityOrderById(1L);

        // Verify the result
        assertNotNull(result);
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(1L, result.getUserId());

        // Verify that the repository method was called
        verify(orderRepository, times(1)).findById(eq(1L));
    }

    @Test
    public void testSaveOrder() {
        // Mock the repository to return the test user when save is called
        when(orderRepository.save(any(EntityOrder.class))).thenReturn(testOrder);

        // Call the service method
        EntityOrder result = orderService.saveEntityOrder(testOrder);

        // Verify the result
        assertNotNull(result);
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(1L, result.getUserId());

        // Verify that the repository method was called
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    public void testDeleteOrder() {
        // Mock the repository to return true when checking for existing user
        when(orderRepository.existsById(1L)).thenReturn(true);

        assertTrue(orderRepository.existsById(1L));
        // Call the service method
        boolean result = orderService.deleteOrder(1L);

        // Verify the result
        assertTrue(result);

        // Verify that the repository methods were called
        verify(orderRepository, times(1)).existsById(1L);
        verify(orderRepository, times(1)).deleteById(1L);
    }

    @Test
    public void testDeleteOrderNotFound() {
        // Mock the repository to return false when checking for existing user
        when(orderRepository.existsById(eq(2L))).thenReturn(false);

        // Call the service method
        boolean result = orderService.deleteOrder(eq(2L));

        // Verify the result
        assertFalse(result);
    }
}
