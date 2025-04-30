package com.example.clpmonitor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.clpmonitor.model.TagLog;

public interface TagLogRepository extends JpaRepository<TagLog, Long>{
    
}
