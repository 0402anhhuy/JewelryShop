package com.hhd.jewelry.service.impl;

import com.hhd.jewelry.entity.Collection;
import com.hhd.jewelry.repository.CollectionRepository;
import com.hhd.jewelry.service.CollectionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CollectionServiceImpl implements CollectionService {
    private final CollectionRepository collectionRepository;

    public CollectionServiceImpl(CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    @Override
    public List<Collection> getAllCollections() {
        return collectionRepository.findAllBy();
    }

    @Override
    public Page<Collection> getCollectionsPage(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return collectionRepository.findAll(pageable);
    }

    @Override
    public Collection getCollectionByName(String collectionName) {
        return collectionRepository.findByName(collectionName).orElse(null);
    }

    @Override
    public Optional<Collection> getCollectionById(Long id) {
        return collectionRepository.findById(id);
    }

    @Override
    public void save(Collection collection) {
        Optional<Collection> existingCollection = collectionRepository.findByName(collection.getName());
        if (existingCollection.isPresent()) {
            Collection existing = existingCollection.get();
            existing.setImageUrl(collection.getImageUrl());
            collectionRepository.save(existing);
        }
        else{
            collectionRepository.save(collection);
        }
    }

    @Override
    public void delete(Collection collection) {

    }

    @Override
    public void deleteById(Long id) {
        collectionRepository.deleteById(id);
    }
}
