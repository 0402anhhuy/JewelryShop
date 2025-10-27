package com.hhd.jewelry.repository;

import com.hhd.jewelry.entity.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionRepository extends JpaRepository<Collection, Long> {
    Optional<Collection> findByName(String collectionName);
    List<Collection> findAllBy();
}
