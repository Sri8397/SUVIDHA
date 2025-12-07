package com.suvidha.backend.repository;

import com.suvidha.backend.model.UserVisit;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserVisitRepository extends MongoRepository<UserVisit, String> {
    Optional<UserVisit> findByUserIdentifier(String userIdentifier);
}
