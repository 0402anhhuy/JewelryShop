package com.hhd.jewelry.service;

import com.hhd.jewelry.entity.Collection;

import java.util.List;
import java.util.Optional;

public interface CollectionService {
    List<Collection> getAllCollections();
    Collection getCollectionByName(String collectionName);
    void save(Collection collection);
    void delete(Collection collection);
}
