package com.mindhub.order_service.services.impl;

import com.mindhub.order_service.dtos.NewOrderDTO;
import com.mindhub.order_service.dtos.OrderDTO;
import com.mindhub.order_service.dtos.UpdateOrderDTO;
import com.mindhub.order_service.models.EntityOrder;
import com.mindhub.order_service.models.OrderStatus;
import com.mindhub.order_service.repositories.OrderRepository;
import com.mindhub.order_service.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;

    private final String USER_SERVICE_URL = "http://localhost:8081/api/user/exists/";

    @Override
    public OrderDTO getOrderDTOById(Long id) {
        return new OrderDTO(getEntityOrderById(id));
    }

    @Override
    public List<OrderDTO> getOrderDTOByStatus(OrderStatus status) {
        List<EntityOrder> orders = orderRepository.findByStatus(status);
        return orders.stream().map(OrderDTO::new).toList();
    }

    @Override
    public EntityOrder getEntityOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order with ID " + id + " not found"));
    }

    @Override
    public EntityOrder saveEntityOrder(EntityOrder order) {
        return orderRepository.save(order);
    }

    @Override
    public boolean createOrder(Long userId, NewOrderDTO newOrder) {
        // Verify if userId exists in user-service
        // Use Boolean.TRUE.equals if getForObject returns null
        try {
            boolean userExists = Boolean.TRUE.equals(restTemplate.getForObject(USER_SERVICE_URL + userId, Boolean.class));
            if (!userExists) {
                throw new RuntimeException("User with ID " + userId + " not found");
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("User with ID " + userId + " not found");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("Error communicating with product-service: " + e.getMessage());
        }
        // Create Order
        EntityOrder order = new EntityOrder(newOrder.status());
        // Associate
        order.setUserId(userId);
        // Save
        saveEntityOrder(order);
        return true;
    }

    @Override
    public List<OrderDTO> getAllOrderDTOs() {
        return orderRepository.findAll().stream()
                .map(OrderDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getAllOrderDTOsByUserId(Long userId) {
        List<EntityOrder> orders = orderRepository.findByUserId(userId);
        return orders.stream().map(OrderDTO::new).toList();
    }

    @Override
    public boolean updateOrder(Long id, UpdateOrderDTO updateOrder) {
        EntityOrder order = getEntityOrderById(id);
        // Update
        order.setStatus(updateOrder.status());
        saveEntityOrder(order);
        return true;
    }

    @Override
    public boolean deleteOrder(Long id) {
        if(!orderRepository.existsById(id)) {
            return false;
        }
        orderRepository.deleteById(id);
        return true;
    }
}
