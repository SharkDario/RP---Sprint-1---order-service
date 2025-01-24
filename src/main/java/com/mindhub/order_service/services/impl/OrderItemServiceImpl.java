package com.mindhub.order_service.services.impl;

import com.mindhub.order_service.dtos.NewOrderItemDTO;
import com.mindhub.order_service.dtos.OrderItemDTO;
import com.mindhub.order_service.dtos.UpdateOrderItemDTO;
import com.mindhub.order_service.models.EntityOrder;
import com.mindhub.order_service.models.OrderItem;
import com.mindhub.order_service.repositories.OrderItemRepository;
import com.mindhub.order_service.repositories.OrderRepository;
import com.mindhub.order_service.services.OrderItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderItemServiceImpl implements OrderItemService {
    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;

    private final String PRODUCT_SERVICE_URL = "http://localhost:8082/api/products/exists/";

    @Override
    public OrderItemDTO getOrderItemDTOById(Long id) {
        return new OrderItemDTO(getOrderItemById(id));
    }

    @Override
    public OrderItem getOrderItemById(Long id) {
        return orderItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order Item with ID " + id + " not found"));
    }

    @Override
    public OrderItem saveOrderItem(OrderItem orderItem) {
        return orderItemRepository.save(orderItem);
    }

    @Override
    public boolean createOrderItem(Long orderId, Long productId, NewOrderItemDTO newOrderItemDTO) {
        // Verify if productId exists in product-service
        // Use Boolean.TRUE.equals if getForObject returns null, converts it to false
        try {
            boolean productExists = Boolean.TRUE.equals(restTemplate.getForObject(PRODUCT_SERVICE_URL + productId, Boolean.class));
            if (!productExists) {
                throw new RuntimeException("Product with ID " + productId + " not found");
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("Product with ID " + productId + " not found");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("Error communicating with product-service: " + e.getMessage());
        }
        // OrderId
        EntityOrder order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order with ID " + orderId + " not found"));
        // Create Order Item
        OrderItem orderItem = new OrderItem(newOrderItemDTO.quantity());
        // Associate
        orderItem.setProductId(productId);
        orderItem.setOrder(order);
        // Save
        saveOrderItem(orderItem);
        return true;
    }

    @Override
    public List<OrderItemDTO> getAllOrderItemDTO() {
        return orderItemRepository.findAll().stream()
                .map(OrderItemDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderItemDTO> getAllOrderItemDTOByOrderId(Long orderId) {
        List<OrderItem> orders = orderItemRepository.findByEntityOrderId(orderId);
        return orders.stream().map(OrderItemDTO::new).toList();
    }

    @Override
    public List<OrderItemDTO> getAllOrderItemDTOByProductId(Long productId) {
        List<OrderItem> orders = orderItemRepository.findByProductId(productId);
        return orders.stream().map(OrderItemDTO::new).toList();
    }

    @Override
    public boolean updateOrderItem(Long id, UpdateOrderItemDTO updateOrderItemDTO) {
        OrderItem orderItem = getOrderItemById(id);
        // Update
        orderItem.setQuantity(updateOrderItemDTO.quantity());
        saveOrderItem(orderItem);
        return true;
    }

    @Override
    public boolean deleteOrderItem(Long id) {
        if(!orderItemRepository.existsById(id)) {
            return false;
        }
        orderItemRepository.deleteById(id);
        return true;
    }
}
