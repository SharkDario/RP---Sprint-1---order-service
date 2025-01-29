package com.mindhub.order_service.models;

import jakarta.persistence.*;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Entity
public class EntityOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @OneToMany(mappedBy = "entityOrder")
    private Set<OrderItem> products = new HashSet<>();

    private OrderStatus status;

    public EntityOrder(OrderStatus status) {
        this.status = status;
    }

    public EntityOrder() {
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Set<OrderItem> getProducts() {
        return products;
    }

    public void setProducts(Set<OrderItem> products) {
        this.products = products;
    }

    public List<OrderItem> getProductsList() {
        return products.stream().collect(Collectors.toList());
    }

    public void setProductsList(List<OrderItem> productsList) {
        this.products = new HashSet<>(productsList);
    }

    public void addProduct(OrderItem orderItem) {
        orderItem.setOrder(this); // DB
        products.add(orderItem); // implicit this. - Not save the reference
    }

    public void removeProduct(OrderItem orderItem) {
        orderItem.setOrder(null); // DB
        products.remove(orderItem); // implicit this. - Not save the reference
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "EntityOrder{" +
                "id=" + id +
                ", userId=" + userId +
                ", products=" + products +
                ", status=" + status +
                '}';
    }
}
