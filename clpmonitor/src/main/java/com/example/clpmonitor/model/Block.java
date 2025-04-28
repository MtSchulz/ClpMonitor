package com.example.clpmonitor.model;

import jakarta.persistence.*;

@Entity
@Table(name = "block")
public class Block {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false)
    private Integer position;
    
    @Column(nullable = false)
    private Integer color;
    
    @ManyToOne
    @JoinColumn(name = "storage_id", nullable = false)
    private Storage storage;
    
    @Column(name = "production_order")
    private String productionOrder;
    
    // Getters e Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Integer getColor() {
        return color;
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public String getProductionOrder() {
        return productionOrder;
    }

    public void setProductionOrder(String productionOrder) {
        this.productionOrder = productionOrder;
    }
}