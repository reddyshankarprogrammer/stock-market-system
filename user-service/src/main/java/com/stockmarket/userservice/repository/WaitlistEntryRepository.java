package com.stockmarket.userservice.repository;

import com.stockmarket.userservice.entity.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, String> {

    boolean existsByEmail(String email);
}
