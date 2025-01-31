package com.mindhub.order_service.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindhub.order_service.dtos.OrderDTO;
import com.mindhub.order_service.dtos.UpdateOrderDTO;
import com.mindhub.order_service.models.EntityOrder;
import com.mindhub.order_service.models.OrderStatus;
import com.mindhub.order_service.services.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// This annotation is used to test Spring MVC controllers,
// focusing only on the web layer
@WebMvcTest(OrderController.class)
public class OrderControllerTest {
    // Autowired to inject MockMvc for simulating HTTP requests
    @Autowired
    private MockMvc mockMvc;
    // MockBean to mock the dependency
    @MockBean
    private OrderService orderService;
    // Autowired to inject ObjectMapper for JSON serialization/deserialization
    @Autowired
    private ObjectMapper objectMapper;
    // Test Order DTO object to be used in tests
    private OrderDTO testOrder;
    // This method runs before each test to set up initial data
    @BeforeEach
    void setUp() {
        // Create a test order and its DTO
        EntityOrder order = new EntityOrder(OrderStatus.PENDING);
        testOrder = new OrderDTO(order);
    }
    // Test to verify that the /api/order/ endpoint returns all the orders
    @Test
    void getAllOrdersShouldReturnOrders() throws Exception {
        // Mock the service to return a list of orders
        List<OrderDTO> orders = Collections.singletonList(testOrder);
        when(orderService.getAllOrderDTOs()).thenReturn(orders);

        // Perform the request and verify the response
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testOrder.getId()))
                .andExpect(jsonPath("$[0].userId").value(testOrder.getUserId()))
                .andExpect(jsonPath("$[0].status").value(testOrder.getStatus().toString()));
    }

    @Test
    void updateOrderShouldUpdateOrder() throws Exception {
        // Create a DTO with updated data
        UpdateOrderDTO updateDto = new UpdateOrderDTO(OrderStatus.COMPLETED);
        // Mock the service to return the test order and confirm the update
        when(orderService.getOrderDTOById(testOrder.getId())).thenReturn(testOrder);
        when(orderService.updateOrder(anyLong(), any(UpdateOrderDTO.class))).thenReturn(true);

        mockMvc.perform(patch("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON) // Set content type to JSON
                        .content(objectMapper.writeValueAsString(updateDto))) // Convert DTO to JSON
                .andExpect(status().isOk()) // Expect HTTP 200 status
                .andExpect(content().string("Order updated successfully")); // Expected message
    }
}
