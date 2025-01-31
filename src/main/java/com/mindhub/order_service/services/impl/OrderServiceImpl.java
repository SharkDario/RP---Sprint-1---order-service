package com.mindhub.order_service.services.impl;

import com.mindhub.order_service.config.JwtUtils;
import com.mindhub.order_service.dtos.*;
import com.mindhub.order_service.exceptions.OrderException;
import com.mindhub.order_service.exceptions.OrderItemException;
import com.mindhub.order_service.exceptions.ProductServiceException;
import com.mindhub.order_service.models.EntityOrder;
import com.mindhub.order_service.models.OrderItem;
import com.mindhub.order_service.models.OrderStatus;
import com.mindhub.order_service.models.ProductError;
import com.mindhub.order_service.repositories.OrderItemRepository;
import com.mindhub.order_service.repositories.OrderRepository;
import com.mindhub.order_service.services.OrderService;
import com.mindhub.order_service.util.Constants;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    // Inject the AmqpTemplate bean to send messages to RabbitMQ
    // order-service is the Producer/Publisher
    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${USERS_PATH}")
    private String userPath; // lb://user-service/api/user -> LoadBalanced in RestTemplate

    @Value("${PRODUCTS_PATH}")
    private String productPath; // lb://product-service/api/products -> LoadBalanced in RestTemplate

    @Override
    public OrderDTO getOrderDTOById(Long id) {
        return new OrderDTO(getEntityOrderById(id));
    }

    @Override
    public OrderDTO getOrderDTOByIdAndUser(Long orderId, Long userId) throws OrderException {
        OrderDTO order = getOrderDTOById(orderId);
        validateOrderOwner(userId,order.getUserId());
        return order;
    }

    // Metodo para validar el token JWT
    private void validateToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid or missing token");
        }

        String jwtToken = token.substring(7); // Eliminar "Bearer " del token

        // Validar el token usando JwtUtils
        if (!jwtUtils.validateToken(jwtToken, jwtUtils.extractUsername(jwtToken))) {
            throw new RuntimeException("Invalid token");
        }
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

    // change
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public OrderCreatedRecord createOrder(String email, NewOrderRecord newOrder) throws OrderException {
        Long userId = getUserIdFromEmail(email);

        HashMap<Long,Integer> existentProductMap = getExistentProducts(newOrder.recordList());

        EntityOrder order = new EntityOrder( OrderStatus.PENDING);
        order.setUserId(userId);
        orderRepository.save(order);

        List<ErrorProductRecord> orderItemsError = setOrderItemList(existentProductMap, newOrder.recordList(), order);

        //updateProducts(order.getProductsList(),-1);

        orderRepository.save(order);

        try{
            updateProducts(order.getProductsList(),-1);
        }catch (ProductServiceException e){
            throw new OrderException(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
        }

        OrderDTO orderDTO = new OrderDTO(order);
        OrderCreatedRecord orderCreatedRecord = new OrderCreatedRecord(orderDTO, orderItemsError);

        // Send a message to RabbitMQ using the AmqpTemplate
        // The message is sent to the "testingExchange" with the routing key "routingUserRegister.key"
        // The payload of the message is the orderCreatedEvent object, with the user's email, order's details and every product's detail
        List<NewProductDTO> productDTOS = getListProducts(newOrder.recordList());
        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(orderDTO.getId(), email, orderDTO.getStatus(), productDTOS);

        if (order.getStatus() == OrderStatus.COMPLETED){
            // To create the PDF with the order details when is COMPLETED:
            amqpTemplate.convertAndSend("testingExchange", "routingOrderCreatedEvent.key", orderCreatedEvent);

        }

        return orderCreatedRecord;

    }

    private List<NewProductDTO> getListProducts(List<ProductQuantityRecord> productQuantityRecordList) throws OrderException {
        ParameterizedTypeReference<List<NewProductDTO>> responseType =
                new ParameterizedTypeReference<>() {};
        HttpEntity<List<ProductQuantityRecord>> httpEntity = new HttpEntity<>(productQuantityRecordList);
        try{
            ResponseEntity<List<NewProductDTO>> responseEntity = restTemplate.exchange(productPath + "/details", HttpMethod.PUT ,httpEntity, responseType);
            return responseEntity.getBody();
        } catch (RestClientException e) {
            throw new OrderException("Error communicating with product-service", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private HashMap<Long,Integer> getExistentProducts(List<ProductQuantityRecord> productQuantityRecordList) throws OrderException {
        ParameterizedTypeReference<HashMap<Long, Integer>> responseType =
                new ParameterizedTypeReference<>() {};
        HttpEntity<List<ProductQuantityRecord>> httpEntity = new HttpEntity<>(productQuantityRecordList);
        try{
            ResponseEntity<HashMap<Long, Integer>> responseEntity = restTemplate.exchange(productPath, HttpMethod.PUT ,httpEntity, responseType);
            return responseEntity.getBody();
        } catch (RestClientException e) {
            throw new OrderException("Error communicating with product-service", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    private List<ErrorProductRecord> setOrderItemList(HashMap<Long, Integer> existentProducts, List<ProductQuantityRecord> wantedProducts, EntityOrder order){
        List<OrderItem> orderItemList = new ArrayList<>();
        List<ErrorProductRecord> errorProductList = new ArrayList<>();
        wantedProducts.forEach(wantedProduct -> {
            if (existentProducts.containsKey(wantedProduct.id())){
                Integer realQuantity = existentProducts.get(wantedProduct.id());
                if (realQuantity>= wantedProduct.quantity()){
                    OrderItem orderItem = new OrderItem(wantedProduct.quantity());
                    orderItem.setOrder(order);
                    orderItem.setProductId(wantedProduct.id());
                    orderItemRepository.save(orderItem);
                    orderItemList.add(orderItem);
                }else{
                    errorProductList.add(new ErrorProductRecord(wantedProduct.id(), ProductError.NO_STOCK));
                }
            }else{
                errorProductList.add(new ErrorProductRecord(wantedProduct.id(), ProductError.NOT_FOUND));
            }
        });

        order.setProductsList(orderItemList);

        return errorProductList;
    }

    private void updateProducts(List<OrderItem> orderItemList, int factor) throws OrderException {

        List<ProductQuantityRecord> productQuantityRecordList = new ArrayList<>();
        orderItemList.forEach(orderItem -> {
            productQuantityRecordList.add(new ProductQuantityRecord(orderItem.getProductId(), factor*orderItem.getQuantity()));
        });

        HttpEntity<List<ProductQuantityRecord>> httpEntity = new HttpEntity<>(productQuantityRecordList);

        try{
            restTemplate.exchange(productPath + "/to-order", HttpMethod.PUT ,httpEntity, String.class);
        } catch (RestClientException e) {
            throw new OrderException("Error communicating with product-service", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    private Long getUserIdFromEmail(String email) throws OrderException {
        try{
            String url = userPath + "/email/" + email;
            Long userId = restTemplate.getForObject(url, Long.class);
            return userId;
        } catch (RestClientException e) {
            if (e instanceof HttpStatusCodeException){
                HttpStatusCodeException aux = (HttpStatusCodeException)e;
                throw new OrderException("User with email " + email + " not found", (HttpStatus) aux.getStatusCode());
            }else{
                throw new OrderException("Error communicating with user-service", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    @Override
    public OrderDTO changeStatus(Long userId, String email, Long id, OrderStatus orderStatus) throws OrderException {
        EntityOrder order = orderRepository.findById(id).orElseThrow(() -> new OrderException("Order not found", HttpStatus.NOT_FOUND));
        validateOrderOwner(userId,order.getUserId());
        order.setStatus(orderStatus);
        order = orderRepository.save(order);
        if (order.getStatus() == OrderStatus.COMPLETED){
            OrderDTO orderDTO = getOrderDTOById(order.getId());
            List<NewProductDTO> productDTOS = getListProducts(orderDTO.getProductQuantityRecords());
            OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(order.getId(), email, order.getStatus(), productDTOS);
            // To create the PDF with the order details when is COMPLETED:
            amqpTemplate.convertAndSend("testingExchange", "routingOrderCreatedEvent.key", orderCreatedEvent);

        }
        return new OrderDTO(order);
    }

    private void sendDataToGeneratePdf(EntityOrder order,String userMail){
        /*List<ProductRecord> listProducts = new ArrayList<>();
        for (OrderItem item : order.getOrderItemList()){
            try {
                ProductRecord product = restTemplate.getForObject(productPath + "/" + item.getProductId(), ProductRecord.class );
                listProducts.add(product);
            }catch (RestClientException e){

            }
        }
        OrderToPdfDTO orderToPdfDTO = new OrderToPdfDTO(order.getId(), order.getUserId(), userMail, listProducts);
        rabbitTemplate.convertAndSend("email-exchange", "user.pdf", orderToPdfDTO);

         */
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

    @Override
    public void deleteOrderUser(Long userId, Long id) throws OrderException {
        OrderDTO orderDTO = getOrderDTOById(id);
        validateOrderOwner(userId,orderDTO.getUserId());
        deleteOrder(id);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public NewOrderItemRecord addOrderItem(Long userId, Long orderId, ProductQuantityRecord productQuantityRecord) throws OrderException, OrderItemException {
        EntityOrder order = orderRepository.findById(orderId).orElseThrow(() -> new OrderException("Order not found", HttpStatus.NOT_FOUND));
        validateOrderOwner(userId,order.getUserId());
        validOrderStatus(order.getId());
        validateOrderItem(order.getId(),productQuantityRecord.id());
        if (productQuantityRecord.quantity()==null || productQuantityRecord.quantity()<0){
            throw new OrderItemException(Constants.INV_QUANTITY);
        }

        List<ProductQuantityRecord> auxList = new ArrayList<>();
        auxList.add(productQuantityRecord);

        HashMap<Long, Integer> existentProductMap = getExistentProducts(auxList);

        if (existentProductMap.containsKey(productQuantityRecord.id())){
            Integer realQuantity = existentProductMap.get(productQuantityRecord.id());
            if (realQuantity>= productQuantityRecord.quantity()){
                OrderItem orderItem = new OrderItem(productQuantityRecord.quantity());
                orderItem.setProductId(productQuantityRecord.id());
                orderItem.setOrder(order);

                List<OrderItem> orderItemList = new ArrayList<>();
                orderItemList.add(orderItem);
                //updateProducts(orderItemList,-1);

                try{
                    updateProducts(orderItemList,-1);
                }catch (ProductServiceException e){
                    throw new OrderException(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
                }

                orderItemRepository.save(orderItem);
                order.addProduct(orderItem);
                orderRepository.save(order);

                return new NewOrderItemRecord(order.getId(), orderItem.getProductId(), orderItem.getQuantity());
            }else{
                throw new OrderException(Constants.NEGATIVE_STOCK, HttpStatus.NOT_FOUND);
            }
        }else{
            throw new OrderException(Constants.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

    }

    @Override
    public Set<NewOrderItemRecord> getAllOrderItemsRecordsByOrderId(Long id) throws OrderException {
        EntityOrder order = orderRepository.findById(id).orElseThrow(() -> new OrderException(Constants.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND));
        Set<NewOrderItemRecord> orderItemSet = order.getProductsList().stream().map(orderItem -> new NewOrderItemRecord(orderItem.getId(),orderItem.getProductId(),orderItem.getQuantity())).collect(Collectors.toSet());
        return orderItemSet;
    }

    private void validateOrderItem(Long orderId,Long orderItemProductId) throws OrderException {
        Set<NewOrderItemRecord> orderItemSet = getAllOrderItemsRecordsByOrderId(orderId);
        Iterator<NewOrderItemRecord> it = orderItemSet.iterator();
        while (it.hasNext()){
            if (it.next().productId()==orderItemProductId){
                throw new OrderException(Constants.ITEM_ALREADY_EXISTS);
            }
        }
    }

    private void validOrderStatus(Long id) throws OrderException {
        OrderDTO order = getOrderDTOById(id);
        if (order.getStatus()==OrderStatus.COMPLETED){
            throw new OrderException(Constants.ORDER_COMPLETED,HttpStatus.UNAUTHORIZED);
        }
    }
    // Changing
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public NewOrderItemRecord updateOrderItemQuantity(Long userId, Long id, Integer quantity) throws OrderItemException, OrderException {
        OrderItem orderItem = orderItemRepository.findById(id).orElseThrow(()->new OrderItemException(Constants.ORDER_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND));
        validateOrderOwner(userId, orderItem.getOrder().getUserId());
        validOrderStatus(orderItem.getOrder().getId());

        int diference = orderItem.getQuantity()-quantity;
        if (quantity>0 && orderItem.getQuantity() != quantity){
            HashMap<Long, Integer> existentProduct = getExistentProducts(List.of(new ProductQuantityRecord(orderItem.getProductId(),quantity)));
            try{
                if (diference>0) {
                    OrderItem newOrderItem = new OrderItem(diference);
                    newOrderItem.setOrder(null);
                    newOrderItem.setProductId(orderItem.getProductId());
                    updateProducts(List.of(newOrderItem), 1);
                }else {
                    if (existentProduct.get(orderItem.getProductId())>= -1*diference){
                        OrderItem newOrderItem = new OrderItem(diference);
                        newOrderItem.setOrder(null);
                        newOrderItem.setProductId(orderItem.getProductId());
                        updateProducts(List.of(newOrderItem), 1);
                    }else{
                        throw new OrderException(Constants.NEGATIVE_STOCK, HttpStatus.NOT_ACCEPTABLE);
                    }
                }
            }catch (ProductServiceException e){
                throw new OrderException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            orderItem.setQuantity(quantity);
            orderItem = orderItemRepository.save(orderItem);
            return new NewOrderItemRecord(orderItem.getId(),orderItem.getProductId(),orderItem.getQuantity());
            /*
            // si la diferencia es positiva: actualizamos el stock del producto pq necesitamos menos
            // negativa: necesitamos mas cantidad del producto
            if (existentProduct.get(id)>=quantity){
                int diference = orderItem.getQuantity()-quantity;
                OrderItem newOrderItem = new OrderItem(diference);
                newOrderItem.setOrder(null);
                newOrderItem.setProductId(id);
                updateProducts(List.of(newOrderItem),1);
                orderItem.setQuantity(quantity);
                orderItem = orderItemRepository.save(orderItem);
                return new NewOrderItemRecord(orderItem.getId(),orderItem.getProductId(),orderItem.getQuantity());
            }else{
                throw new OrderException(Constants.NEGATIVE_STOCK, HttpStatus.NOT_ACCEPTABLE);
            }
             */
        }else{
            throw new OrderException(Constants.INV_QUANTITY, HttpStatus.NOT_ACCEPTABLE);
        }

    }

    @Override
    public boolean existsOrderItem(Long id) {
        return orderItemRepository.existsById(id);
    }

    private void validateOrderOwner(Long userId, Long orderId) throws OrderException {
        if (!Objects.equals(userId, orderId)){
            throw new OrderException(Constants.NOT_PERM, HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public void deleteOrderItem(Long userId, Long orderItemId) throws OrderItemException, OrderException {
        OrderItem orderItem = orderItemRepository.findById(orderItemId).orElseThrow(()->new OrderItemException(Constants.ORDER_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND));
        validateOrderOwner(userId, orderItem.getOrder().getUserId());

        validOrderStatus(orderItem.getOrder().getId());

        List<OrderItem> orderItemList = new ArrayList<>();
        orderItemList.add(orderItem);

        updateProducts(orderItemList,1);

        orderItemRepository.delete(orderItem);
    }
}