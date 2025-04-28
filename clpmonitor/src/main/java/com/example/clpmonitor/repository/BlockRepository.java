package com.example.clpmonitor.repository;

import com.example.clpmonitor.model.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BlockRepository extends JpaRepository<Block, Integer> {
    List<Block> findByStorageIdOrderByPositionAsc(Integer storageId);
    List<Block> findByProductionOrder(String productionOrder);
}