package com.example.clpmonitor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.clpmonitor.model.Block;
import com.example.clpmonitor.model.Storage;

public interface BlockRepository extends JpaRepository<Block, Integer> {

    List<Block> findByStorageIdOrderByPositionAsc(Integer storageId);

    List<Block> findByProductionOrder(String productionOrder);

    Optional<Block> findByStorageAndPosition(Storage storage, Integer position);
}
