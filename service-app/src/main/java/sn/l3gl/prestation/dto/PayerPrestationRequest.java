package sn.l3gl.prestation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PayerPrestationRequest {
    @NotBlank(message = "Le PIN est obligatoire !")
    private String pin;
}
