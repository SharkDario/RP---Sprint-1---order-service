package com.mindhub.order_service.services;

import com.mindhub.order_service.dtos.NewOrderItemDTO;
import com.mindhub.order_service.dtos.OrderItemDTO;
import com.mindhub.order_service.dtos.UpdateOrderItemDTO;
import com.mindhub.order_service.models.OrderItem;

import java.util.List;

public interface OrderItemService {
    OrderItemDTO getOrderItemDTOById(Long id);

    OrderItem getOrderItemById(Long id);

    OrderItem saveOrderItem(OrderItem orderItem);

    boolean createOrderItem(Long orderId, Long productId, NewOrderItemDTO newOrderItemDTO);

    List<OrderItemDTO> getAllOrderItemDTO();
    List<OrderItemDTO> getAllOrderItemDTOByOrderId(Long orderId);
    List<OrderItemDTO> getAllOrderItemDTOByProductId(Long productId);

    boolean updateOrderItem(Long id, UpdateOrderItemDTO updateOrderItemDTO);

    public boolean deleteOrderItem(Long id);
}
