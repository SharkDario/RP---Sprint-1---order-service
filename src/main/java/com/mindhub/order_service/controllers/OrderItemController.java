package com.mindhub.order_service.controllers;

import com.mindhub.order_service.config.JwtUtils;
import com.mindhub.order_service.dtos.*;
import com.mindhub.order_service.exceptions.OrderException;
import com.mindhub.order_service.exceptions.OrderItemException;
import com.mindhub.order_service.services.OrderService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/orderItems")
public class OrderItemController {
    // Dependencies Injection - Only things that are in the context of Spring Boot (has to be Component)
    // From behind generates a constructor and injects the bean for this repository (interface)
    //@Autowired
    //private OrderItemService orderItemService; // inject the interface directly

    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtils jwtUtils;

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

    /*
     * Retrieve all order items by order ID.
     * @param orderId The ID of the order.
     * @return A set of {@link OrderItemRecord} objects associated with the order.
     * @throws OrderException If the order does not exist.
     * @response 200 OK - List of order items for the specified order.
     */
    @GetMapping("/user/{orderId}")
    public ResponseEntity<Set<NewOrderItemRecord>> getAllOrderItemsByOrderId(@PathVariable Long orderId) throws OrderException {
        Set<NewOrderItemRecord> orderItems = orderService.getAllOrderItemsRecordsByOrderId(orderId);
        return ResponseEntity.ok(orderItems);
    }

    /*
     * Add a new order item to an order.
     * @param newOrderItem The {@link NewOrderItemRecord} object containing details of the new order item.
     * @throws OrderException If the order does not exist.
     * @throws OrderItemException If there is an issue with the new order item (e.g., invalid product ID).
     * @response 201 Created - Order item successfully added.
     */
    @PostMapping("/user/{orderId}")
    public ResponseEntity<NewOrderItemRecord> addOrderItem(HttpServletRequest request, @PathVariable Long orderId, @RequestBody ProductQuantityRecord newOrderItem) throws OrderException, OrderItemException {
        Long userId  = jwtUtils.getIdFromToken(request.getHeader("Authorization"));
        NewOrderItemRecord orderItemRecord = orderService.addOrderItem(userId, orderId, newOrderItem);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderItemRecord);
    }

    /*
     * Update an order item's quantity.
     * @param orderItemId The ID of the order item to update.
     * @param updateOrderItemRecord An object containing the updated quantity for the order item.
     * @return The updated {@link OrderItemRecord}.
     * @throws OrderItemException If the order item does not exist or the quantity is invalid.
     * @response 200 OK - Order item successfully updated.
     */
    @PutMapping("/user/{orderItemId}")
    public ResponseEntity<NewOrderItemRecord> updateOrderItem(HttpServletRequest request, @PathVariable Long orderItemId, @RequestBody @Valid UpdateOrderItemDTO updateOrderItemRecord) throws OrderItemException, OrderException {
        Long userId  = jwtUtils.getIdFromToken(request.getHeader("Authorization"));
        NewOrderItemRecord orderItems = orderService.updateOrderItemQuantity(userId, orderItemId, updateOrderItemRecord.quantity());
        return ResponseEntity.ok(orderItems);
    }

    /*
     * Delete an order item by its ID.
     * @param orderItemId The ID of the order item to delete.
     * @throws OrderException If the associated order does not exist.
     * @throws OrderItemException If the order item does not exist.
     * @response 204 No Content - Order item successfully deleted.
     */
    @DeleteMapping("/user/{orderItemId}")
    public ResponseEntity<Void> deleteOrderItem(@PathVariable Long orderItemId, HttpServletRequest request) throws OrderException, OrderItemException {
        Long userId = jwtUtils.getIdFromToken(request.getHeader("Authorization"));
        orderService.deleteOrderItem(userId, orderItemId);
        return ResponseEntity.noContent().build();
    }
}
