package com.hhd.jewelry.service;

import com.hhd.jewelry.entity.Collection;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface CollectionService {
    List<Collection> getAllCollections();
    Page<Collection> getCollectionsPage(int page, int size);
    Collection getCollectionByName(String collectionName);
    Optional<Collection> getCollectionById(Long id);
    void save(Collection collection);
    void delete(Collection collection);
    void deleteById(Long id);
}
