package com.mindhub.order_service.services;

import com.mindhub.order_service.dtos.NewOrderDTO;
import com.mindhub.order_service.dtos.NewOrderRecord;
import com.mindhub.order_service.dtos.OrderDTO;
import com.mindhub.order_service.dtos.UpdateOrderDTO;
import com.mindhub.order_service.exceptions.OrderException;
import com.mindhub.order_service.models.EntityOrder;
import com.mindhub.order_service.models.OrderStatus;
import org.hibernate.query.Order;

import java.util.List;
import java.util.Map;

public interface OrderService {
    OrderDTO getOrderDTOById(Long id);
    List<OrderDTO> getOrderDTOByStatus(OrderStatus status);

    EntityOrder getEntityOrderById(Long id);

    EntityOrder saveEntityOrder(EntityOrder order);

    OrderDTO createOrder(NewOrderRecord newOrder) throws OrderException;
    //Map<OrderDTO, String> createOrder(NewOrderRecord newOrder) throws OrderException;

    List<OrderDTO> getAllOrderDTOs();
    List<OrderDTO> getAllOrderDTOsByUserId(Long userId);

    boolean updateOrder(Long id, UpdateOrderDTO updateOrder);

    public boolean deleteOrder(Long id);
}
