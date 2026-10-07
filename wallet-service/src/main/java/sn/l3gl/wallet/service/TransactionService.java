package sn.l3gl.wallet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sn.l3gl.wallet.exception.PaiementDejaTraiteException;
import sn.l3gl.wallet.model.Transaction;
import sn.l3gl.wallet.repository.TransactionRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public List<Transaction> historique(Long compteId) {
        return transactionRepository.findByCompteIdOrderByDateTransactionDesc(compteId);
    }

    public Optional<Transaction> findByDemandeId(String demandeId) {
        return transactionRepository.findByDemandeId(demandeId);
    }

    /**
     * Garde d'idempotence : un même demandeId (issu de service-app) ne peut
     * déclencher qu'un seul débit, même si l'appel HTTP est rejoué après timeout.
     */
    public void verifierNonDejaTraite(String demandeId) {
        if (transactionRepository.existsByDemandeId(demandeId)) {
            throw new PaiementDejaTraiteException();
        }
    }
}
