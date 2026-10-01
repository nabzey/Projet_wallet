package sn.l3gl.wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.l3gl.wallet.model.Transaction;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCompteIdOrderByDateTransactionDesc(Long compteId);
    boolean existsByDemandeId(String demandeId);
    Optional<Transaction> findByDemandeId(String demandeId);
}
