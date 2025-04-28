package com.example.clpmonitor.repository;

import com.example.clpmonitor.model.Storage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageRepository extends JpaRepository<Storage, Integer> {
}