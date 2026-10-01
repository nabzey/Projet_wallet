package sn.l3gl.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaiementEffectueEvent {
    private String demandeId;
    private String statut; // SUCCESS ou ECHEC
    private String message;
}
