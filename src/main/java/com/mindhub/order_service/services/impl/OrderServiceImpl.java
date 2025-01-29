package com.mindhub.order_service.services.impl;

import com.mindhub.order_service.dtos.*;
import com.mindhub.order_service.exceptions.OrderException;
import com.mindhub.order_service.exceptions.OrderItemException;
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

    @Value("${USERS_PATH}")
    private String userPath; // lb://user-service/api/user -> LoadBalanced in RestTemplate

    @Value("${PRODUCTS_PATH}")
    private String productPath; // lb://product-service/api/products -> LoadBalanced in RestTemplate

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
    public OrderCreatedRecord createOrder(NewOrderRecord newOrder) throws OrderException {
        Long userId = getUserIdFromEmail(newOrder.email());

        HashMap<Long,Integer> existentProductMap = getExistentProducts(newOrder.recordList());

        EntityOrder order = new EntityOrder( OrderStatus.PENDING);
        order.setUserId(userId);
        orderRepository.save(order);

        List<ErrorProductRecord> orderItemsError = setOrderItemList(existentProductMap, newOrder.recordList(), order);

        updateProducts(order.getProductsList(),-1);

        orderRepository.save(order);

        OrderDTO orderDTO = new OrderDTO(order);
        OrderCreatedRecord orderCreatedRecord = new OrderCreatedRecord(orderDTO, orderItemsError);

        // Send a message to RabbitMQ using the AmqpTemplate
        // The message is sent to the "testingExchange" with the routing key "routingUserRegister.key"
        // The payload of the message is the orderCreatedEvent object, with the user's email, order's details and every product's detail
        List<NewProductDTO> productDTOS = getListProducts(newOrder.recordList());
        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(orderDTO.getId(), newOrder.email(), orderDTO.getStatus(), productDTOS);
        amqpTemplate.convertAndSend("testingExchange", "routingOrderCreatedEvent.key", orderCreatedEvent);

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

    /*
    @Override
    public OrderCreatedRecord createOrder(NewOrderRecord newOrder) throws OrderException {
        String uri = "/email/" + newOrder.email();
        try{
            Long userId = restTemplate.getForObject(userPath + uri, Long.class);
            ParameterizedTypeReference<List<ExistentProductsRecord>> responseType =
                    new ParameterizedTypeReference<>() {};
            HttpEntity<List<ProductQuantityRecord>> httpEntity = new HttpEntity<>(newOrder.recordList());
            try {
                ResponseEntity<List<ExistentProductsRecord>> responseEntity = restTemplate.exchange(productPath, HttpMethod.PUT, httpEntity, responseType);

                EntityOrder order = new EntityOrder(OrderStatus.PENDING);
                order.setUserId(userId);
                orderRepository.save(order);

                generateOrderItemList(responseEntity.getBody(), order);

                List<ErrorProductRecord> errorList = generateErrorProductList(newOrder.recordList(), responseEntity.getBody());

                orderRepository.save(order);

                OrderDTO orderDTO = new OrderDTO(order);

                // Intento
                OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(orderDTO.getId(), newOrder.email(), orderDTO.getStatus());

                amqpTemplate.convertAndSend("testingExchange", "routingOrderCreatedEvent.key", orderCreatedEvent);

                return new OrderCreatedRecord(orderDTO, errorList);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                throw new RuntimeException("Error communicating with product-service: " + e.getMessage());
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("User with email " + newOrder.email() + " not found");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("Error communicating with user-service: " + e.getMessage());
        }
    }

    private void generateOrderItemList(List<ExistentProductsRecord> productQuantityList, EntityOrder order){
        //List<OrderItem> orderItemList = new ArrayList<>();
        Iterator<ExistentProductsRecord> it = productQuantityList.iterator();
        while (it.hasNext()){
            ExistentProductsRecord aux = it.next();
            if(aux.price()!=null) {
                OrderItem orderItem = new OrderItem(aux.quantity());
                orderItem.setProductId(aux.id());
                order.addProduct(orderItem);
                //orderItem.setOrder(order);
                orderItemRepository.save(orderItem);
            }
            //orderItemList.add(orderItem);
        }
    }

    private String generateErrorMessage(List<ProductQuantityRecord> productsUser, List<ExistentProductsRecord> productsAvailable) {
        return productsUser.stream()
                .filter(userProduct ->
                        !productsAvailable.stream().anyMatch(availableProduct ->
                                availableProduct.id().equals(userProduct.id()) && availableProduct.price() != null)
                )
                .map(userProduct -> {
                    boolean productExists = productsAvailable.stream()
                            .anyMatch(p -> p.id().equals(userProduct.id()));

                    return productExists
                            ? "Product with ID " + userProduct.id() + " doesn't have stock for the quantity " + userProduct.quantity()
                            : "Product with ID " + userProduct.id() + " doesn't exist";
                })
                .collect(Collectors.joining(","));
    }

    private List<ErrorProductRecord> generateErrorProductList(List<ProductQuantityRecord> userProductsList,List<ExistentProductsRecord> existentProductsList){
        List<ErrorProductRecord> errorProductList = new ArrayList<>();
        List<ProductQuantityRecord> aux = userProductsList.stream()
                .filter(userProduct ->
                        !existentProductsList.stream().anyMatch(availableProduct ->
                                availableProduct.id().equals(userProduct.id()) && availableProduct.price() != null)
                ).toList();

        aux.forEach(product -> {
            boolean productExists = existentProductsList.stream()
                    .anyMatch(p -> p.id().equals(product.id()));
            if (productExists) {
                errorProductList.add(new ErrorProductRecord(product.id(), ProductError.NO_STOCK));
            } else {
                errorProductList.add(new ErrorProductRecord(product.id(), ProductError.NOT_FOUND));
            }
        });

        return  errorProductList;
    }
    */

    @Override
    public OrderDTO changeStatus(Long id, OrderStatus orderStatus) throws OrderException {
        EntityOrder order = orderRepository.findById(id).orElseThrow(() -> new OrderException("Order not found", HttpStatus.NOT_FOUND));
        order.setStatus(orderStatus);
        order = orderRepository.save(order);
        return new OrderDTO(order);
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
    public NewOrderItemRecord addOrderItem(Long OrderId, ProductQuantityRecord productQuantityRecord) throws OrderException, OrderItemException {
        EntityOrder order = orderRepository.findById(OrderId).orElseThrow(() -> new OrderException("Order not found", HttpStatus.NOT_FOUND));
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
                updateProducts(orderItemList,-1);

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

    @Override
    public NewOrderItemRecord updateOrderItemQuantity(Long id, Integer quantity) throws OrderItemException, OrderException {
        OrderItem orderItem = orderItemRepository.findById(id).orElseThrow(()->new OrderItemException(Constants.ORDER_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND));
        validOrderStatus(orderItem.getOrder().getId());

        if (quantity>0 && orderItem.getQuantity() != quantity){
            HashMap<Long, Integer> existentProduct = getExistentProducts(List.of(new ProductQuantityRecord(id,quantity)));

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
        }else{
            throw new OrderException(Constants.INV_QUANTITY, HttpStatus.NOT_ACCEPTABLE);
        }

    }

    @Override
    public boolean existsOrderItem(Long id) {
        return orderItemRepository.existsById(id);
    }
}
