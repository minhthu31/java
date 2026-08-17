package com.example.requirement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RequirementRepository extends JpaRepository<Requirement, String>, JpaSpecificationExecutor<Requirement> {
    Optional<Requirement> findByIdAndIsDeletedFalse(String id);
}
