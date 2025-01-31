package com.mindhub.order_service.services;

import com.mindhub.order_service.dtos.*;
import com.mindhub.order_service.exceptions.OrderException;
import com.mindhub.order_service.exceptions.OrderItemException;
import com.mindhub.order_service.models.EntityOrder;
import com.mindhub.order_service.models.OrderStatus;
import org.hibernate.query.Order;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface OrderService {
    OrderDTO getOrderDTOById(Long id);
    OrderDTO getOrderDTOByIdAndUser(Long orderId, Long userId) throws OrderException;
    List<OrderDTO> getOrderDTOByStatus(OrderStatus status);

    EntityOrder getEntityOrderById(Long id);

    EntityOrder saveEntityOrder(EntityOrder order);

    //OrderDTO createOrder(NewOrderRecord newOrder) throws OrderException;
    OrderCreatedRecord createOrder(String email, NewOrderRecord newOrder) throws OrderException;
    OrderDTO changeStatus(Long userId, String email, Long id, OrderStatus orderStatus) throws OrderException;

    List<OrderDTO> getAllOrderDTOs();
    List<OrderDTO> getAllOrderDTOsByUserId(Long userId);

    boolean updateOrder(Long id, UpdateOrderDTO updateOrder);

    public boolean deleteOrder(Long id);

    Set<NewOrderItemRecord> getAllOrderItemsRecordsByOrderId(Long id) throws OrderException;
    //List<NewOrderItemDTO> getAllOrderItemsByOrderId(Long id) throws OrderException;
    public NewOrderItemRecord addOrderItem(Long userId, Long orderId, ProductQuantityRecord productQuantityRecord) throws OrderException, OrderItemException;

    public NewOrderItemRecord updateOrderItemQuantity(Long userId, Long id, Integer quantity) throws OrderException, OrderItemException;
    boolean existsOrderItem(Long id);
    void deleteOrderUser(Long userId, Long id) throws OrderException;

    public void deleteOrderItem(Long userId, Long orderItemId) throws OrderItemException, OrderException;
}
