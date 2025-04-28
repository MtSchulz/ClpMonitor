package com.example.clpmonitor.model;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "production_order", nullable = false)
    private String productionOrder;
    
    // Getters e Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getProductionOrder() {
        return productionOrder;
    }

    public void setProductionOrder(String productionOrder) {
        this.productionOrder = productionOrder;
    }
}