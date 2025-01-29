package com.mindhub.order_service.controllers;

import com.mindhub.order_service.dtos.*;
import com.mindhub.order_service.exceptions.OrderException;
import com.mindhub.order_service.exceptions.OrderItemException;
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

@RestController
@RequestMapping("/api/orderItems")
public class OrderItemController {
    // Dependencies Injection - Only things that are in the context of Spring Boot (has to be Component)
    // From behind generates a constructor and injects the bean for this repository (interface)
    @Autowired
    private OrderItemService orderItemService; // inject the interface directly

    @Autowired
    private OrderService orderService;

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

    // POST /orderItems: Create an orderItem.
    @PostMapping("/{orderId}/{productId}")
    public ResponseEntity<?> createOrderItem(@PathVariable Long orderId, @PathVariable Long productId, @Valid @RequestBody NewOrderItemDTO newOrderItemDTO) {
        orderItemService.createOrderItem(orderId, productId, newOrderItemDTO);
        return new ResponseEntity<>("Order Item created successfully", HttpStatus.CREATED);
    }

    /*
     * Add a new order item to an order.
     * @param newOrderItem The {@link NewOrderItemRecord} object containing details of the new order item.
     * @throws OrderException If the order does not exist.
     * @throws OrderItemException If there is an issue with the new order item (e.g., invalid product ID).
     * @response 201 Created - Order item successfully added.
     */
    @PostMapping("/{orderId}")
    public ResponseEntity<NewOrderItemRecord> addOrderItem(@PathVariable Long orderId,@RequestBody ProductQuantityRecord newOrderItem) throws OrderException, OrderItemException {
        NewOrderItemRecord orderItemRecord = orderService.addOrderItem(orderId, newOrderItem);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderItemRecord);
    }

    // GET /orderItems: Get all orderItems.
    @GetMapping
    public ResponseEntity<List<OrderItemDTO>> getAllOrderItems() {
        List<OrderItemDTO> orderItems = orderItemService.getAllOrderItemDTO();
        return new ResponseEntity<>(orderItems, HttpStatus.OK);
    }

    // PATCH /orderItems/{id}: Update an orderItem (status)
    /*
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateOrderItem(@PathVariable Long id, @Valid @RequestBody UpdateOrderItemDTO updateOrderItemDTO) {
        try {
            orderItemService.updateOrderItem(id, updateOrderItemDTO);
            return new ResponseEntity<>("Order Item updated successfully", HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
     */

    /*
     * Update an order item's quantity.
     * @param orderItemId The ID of the order item to update.
     * @param updateOrderItemRecord An object containing the updated quantity for the order item.
     * @return The updated {@link OrderItemRecord}.
     * @throws OrderItemException If the order item does not exist or the quantity is invalid.
     * @response 200 OK - Order item successfully updated.
     */
    @PutMapping("/{orderItemId}")
    public ResponseEntity<NewOrderItemRecord> updateOrderItem(@PathVariable Long orderItemId, @RequestBody UpdateOrderItemDTO updateOrderItemRecord) throws OrderItemException, OrderException {
        NewOrderItemRecord orderItems = orderService.updateOrderItemQuantity(orderItemId, updateOrderItemRecord.quantity());
        return ResponseEntity.ok(orderItems);
    }

    // DELETE /orderItems/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrderItem(@PathVariable Long id) {
        boolean deleted = orderItemService.deleteOrderItem(id);
        if (!deleted) {
            return new ResponseEntity<>("Order Item not found", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>("Order Item deleted successfully", HttpStatus.OK);
    }
}
