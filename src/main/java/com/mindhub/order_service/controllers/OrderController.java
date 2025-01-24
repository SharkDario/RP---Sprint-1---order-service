package com.mindhub.order_service.controllers;

import com.mindhub.order_service.dtos.NewOrderDTO;
import com.mindhub.order_service.dtos.NewOrderRecord;
import com.mindhub.order_service.dtos.OrderDTO;
import com.mindhub.order_service.dtos.UpdateOrderDTO;
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

    // POST /orders: Create an order.
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody NewOrderRecord newOrderDTO) throws OrderException {
        OrderDTO createdOrder = orderService.createOrder(newOrderDTO);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }


    /*
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody NewOrderRecord newOrderDTO) throws OrderException {
        Map<OrderDTO, String> orderMap = orderService.createOrder(newOrderDTO);
        OrderDTO createdOrder = orderMap.keySet().iterator().next();
        String errorMessage = orderMap.get(createdOrder);

        // Devuelve el OrderDTO en el cuerpo y el mensaje en un header
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Error-Message", errorMessage) // Agrega el mensaje como header
                .body(createdOrder); // Devuelve el OrderDTO en el cuerpo
    }

     */

    // GET /orders: Get all orders.
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<OrderDTO> orders = orderService.getAllOrderDTOs();
        return new ResponseEntity<>(orders, HttpStatus.OK);
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
