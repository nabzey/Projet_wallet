package sn.l3gl.wallet.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class DepotRequest {
    @Min(value = 1, message = "Le montant doit être supérieur à 0 !")
    private long montant;
}
