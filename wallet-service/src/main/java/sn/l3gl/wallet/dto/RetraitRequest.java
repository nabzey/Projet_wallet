package sn.l3gl.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RetraitRequest {
    @Min(value = 1, message = "Le montant doit être supérieur à 0 !")
    private long montant;

    @NotBlank(message = "Le PIN est obligatoire !")
    private String pin;
}
