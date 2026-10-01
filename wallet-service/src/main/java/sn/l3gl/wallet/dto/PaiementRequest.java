package sn.l3gl.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaiementRequest {
    @Min(value = 1, message = "Le montant doit être supérieur à 0 !")
    private long montant;

    @NotBlank(message = "Le PIN est obligatoire !")
    private String pin;

    /**
     * Identifiant de la demande de service (service-app), utilisé comme clé
     * d'idempotence pour éviter un double débit en cas de retry réseau.
     */
    @NotBlank(message = "L'identifiant de la demande est obligatoire !")
    private String demandeId;
}
