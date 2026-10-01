package sn.l3gl.prestation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Corps envoyé à wallet-service (POST /api/transactions/paiement) pour initier
 * le débit correspondant à une prestation.
 */
@Data
@AllArgsConstructor
public class PaiementRequestWallet {
    private long montant;
    private String pin;
    private String demandeId;
}
