package com.example.clpmonitor.repository;

import com.example.clpmonitor.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Integer> {
}