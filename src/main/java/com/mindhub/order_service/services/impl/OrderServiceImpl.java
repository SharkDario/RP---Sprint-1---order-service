package com.mindhub.order_service.services.impl;

import com.mindhub.order_service.dtos.*;
import com.mindhub.order_service.exceptions.OrderException;
import com.mindhub.order_service.models.EntityOrder;
import com.mindhub.order_service.models.OrderItem;
import com.mindhub.order_service.models.OrderStatus;
import com.mindhub.order_service.models.ProductError;
import com.mindhub.order_service.repositories.OrderItemRepository;
import com.mindhub.order_service.repositories.OrderRepository;
import com.mindhub.order_service.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${USERS_PATH}")
    private String userPath;

    @Value("${PRODUCTS_PATH}")
    private String productPath;

    //private final String USER_SERVICE_URL = "http://localhost:8081/api/user/exists/";

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

                return new OrderCreatedRecord(orderDTO, errorList);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                throw new RuntimeException("Error communicating with product-service: " + e.getMessage());
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("User with email " + newOrder.email() + " not found");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("Error communicating with product-service: " + e.getMessage());
        }
    }


    /*
    @Override
    public OrderDTO createOrder(NewOrderRecord newOrder) throws OrderException {
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

                orderRepository.save(order);

                OrderDTO orderDTO = new OrderDTO(order);

                return orderDTO;
            } catch (HttpClientErrorException.NotFound e) {
                    throw new RuntimeException("Products not found");
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                    throw new RuntimeException("Error communicating with product-service: " + e.getMessage());
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("User with email " + newOrder.email() + " not found");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("Error communicating with user-service: " + e.getMessage());
        }
    }

     */

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



    /*
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

     */

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
