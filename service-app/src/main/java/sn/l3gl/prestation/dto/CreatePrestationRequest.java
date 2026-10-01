package sn.l3gl.prestation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePrestationRequest {
    @NotBlank(message = "Le titre est obligatoire !")
    private String titre;

    private String description;

    @Min(value = 1, message = "Le montant doit être supérieur à 0 !")
    private long montant;
}
