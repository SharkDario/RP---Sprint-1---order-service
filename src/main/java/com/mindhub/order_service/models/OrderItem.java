package com.mindhub.order_service.models;

import jakarta.persistence.*;

@Entity
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private Integer quantity;

    @ManyToOne //@JoinColumn(name = "entity_order_id")
    private EntityOrder entityOrder;

    public OrderItem(Integer quantity) {
        this.quantity = quantity;
    }

    public OrderItem() {
    }

    public Long getId() {
        return id;
    }

    public EntityOrder getOrder() {
        return entityOrder;
    }

    public void setOrder(EntityOrder order) {
        this.entityOrder = order;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", order=" + entityOrder +
                ", productId=" + productId +
                ", quantity=" + quantity +
                '}';
    }
}
