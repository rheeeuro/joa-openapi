package com.joa.openapi.transaction.repository;

import com.joa.openapi.bank.entity.Bank;
import com.joa.openapi.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByDummyId(UUID uuid);
}
