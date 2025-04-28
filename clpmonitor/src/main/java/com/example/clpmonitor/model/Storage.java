package com.example.clpmonitor.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "storage")
public class Storage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(nullable = false)
    private Integer capacity;
    
    @OneToMany(mappedBy = "storage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Block> blocks;

    // Getters e Setters
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }
        
}