package sn.l3gl.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TransactionResponse {
    private String reference;
    private long montant;
    private String type;
    private long nouveauSolde;
    private LocalDateTime dateTransaction;
}
