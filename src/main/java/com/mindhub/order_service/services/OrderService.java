package com.mindhub.order_service.services;

import com.mindhub.order_service.dtos.NewOrderDTO;
import com.mindhub.order_service.dtos.OrderDTO;
import com.mindhub.order_service.dtos.UpdateOrderDTO;
import com.mindhub.order_service.models.EntityOrder;
import com.mindhub.order_service.models.OrderStatus;

import java.util.List;

public interface OrderService {
    OrderDTO getOrderDTOById(Long id);
    List<OrderDTO> getOrderDTOByStatus(OrderStatus status);

    EntityOrder getEntityOrderById(Long id);

    EntityOrder saveEntityOrder(EntityOrder order);

    boolean createOrder(Long userId, NewOrderDTO newOrder);

    List<OrderDTO> getAllOrderDTOs();
    List<OrderDTO> getAllOrderDTOsByUserId(Long userId);

    boolean updateOrder(Long id, UpdateOrderDTO updateOrder);

    public boolean deleteOrder(Long id);
}
