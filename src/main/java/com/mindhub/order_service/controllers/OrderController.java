package com.mindhub.order_service.controllers;

import com.mindhub.order_service.dtos.*;
import com.mindhub.order_service.exceptions.OrderException;
import com.mindhub.order_service.services.OrderItemService;
import com.mindhub.order_service.services.OrderService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    // Dependencies Injection - Only things that are in the context of Spring Boot (has to be Component)
    // From behind generates a constructor and injects the bean for this repository (interface)
    @Autowired
    private OrderService orderService; // inject the interface directly

    // Validate errors
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }
    // Validate business exceptions
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(EntityNotFoundException.class)
    public Map<String, String> handleEntityNotFound(EntityNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return error;
    }

    // Validate general exceptions
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Map<String, String> handleGeneralExceptions(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "An unexpected error occurred: " + ex.getMessage());
        return error;
    }

    @PostMapping
    public ResponseEntity<OrderCreatedRecord> createOrder(@RequestBody NewOrderRecord newOrderDTO) throws OrderException {
        OrderCreatedRecord createdOrder = orderService.createOrder(newOrderDTO);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    // GET /orders: Get all orders.
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<OrderDTO> orders = orderService.getAllOrderDTOs();
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    // GET order by id
    // GET de todas las ordenes de un user
    /*
     * Retrieve all orders of a specific user.
     * @param userId The ID of the user.
     * @return A set of {@link OrderDTO} objects associated with the user.
     * @response 200 OK - List of the user's orders.
     */
    @GetMapping("/all/{userId}")
    public ResponseEntity<List<OrderDTO>> getAllOrdersByUserId(@PathVariable Long userId) {
        List<OrderDTO> orders = orderService.getAllOrderDTOsByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    // PATCH /orders/{id}: Update an order (status)
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateOrder(@PathVariable Long id, @Valid @RequestBody UpdateOrderDTO updateOrderDTO) {
        try {
            orderService.updateOrder(id, updateOrderDTO);
            return new ResponseEntity<>("Order updated successfully", HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /*
     * Update the status of an order.
     * @param orderId The ID of the order.
     * @param updateOrderRecord An object containing the new order status.
     * @return The updated order as a {@link OrderDTO}.
     * @throws OrderException If the order does not exist or the status is invalid.
     * @response 200 OK - Order successfully updated.
     */
    @PutMapping("/{orderId}")
    public ResponseEntity<OrderDTO> changeStatus(@PathVariable Long orderId, @RequestBody UpdateOrderDTO updateOrderRecord) throws OrderException {
        OrderDTO orderDTO = orderService.changeStatus(orderId, updateOrderRecord.status());
        return new ResponseEntity<>(orderDTO, HttpStatus.CREATED);
    }

    // DELETE /orders/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable Long id) {
        boolean deleted = orderService.deleteOrder(id);
        if (!deleted) {
            return new ResponseEntity<>("Order not found", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>("Order deleted successfully", HttpStatus.OK);
    }
}
