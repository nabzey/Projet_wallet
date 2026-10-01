package sn.l3gl.prestation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AffecterRessourceRequest {
    @NotBlank(message = "Le nom est obligatoire !")
    private String nom;

    @NotBlank(message = "La spécialité est obligatoire !")
    private String specialite;
}
