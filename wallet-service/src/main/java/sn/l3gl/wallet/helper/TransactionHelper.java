package sn.l3gl.wallet.helper;

import org.springframework.stereotype.Component;
import sn.l3gl.wallet.model.Compte;
import sn.l3gl.wallet.model.Transaction;
import sn.l3gl.wallet.model.TypeTransaction;

import java.util.UUID;

/**
 * Calculs métier purs, sans accès DB — testable unitairement sans mock.
 */
@Component
public class TransactionHelper {

    public Transaction construire(Compte compte, long montant, TypeTransaction type, String demandeId) {
        Transaction transaction = new Transaction();
        transaction.setReference(genererReference());
        transaction.setMontant(montant);
        transaction.setType(type);
        transaction.setDemandeId(demandeId);
        transaction.setCompte(compte);
        return transaction;
    }

    public String genererReference() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    public String genererNumeroCompte() {
        return "CPT-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }
}
