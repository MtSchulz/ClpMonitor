package com.example.clpmonitor.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.clpmonitor.model.Order;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    Optional<Order> findByProductionOrder(String productionOrder);

}
