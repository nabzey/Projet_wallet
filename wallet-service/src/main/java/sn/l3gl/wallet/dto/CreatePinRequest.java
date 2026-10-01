package sn.l3gl.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreatePinRequest {
    @NotBlank(message = "Le numéro de téléphone est obligatoire !")
    private String telephone;

    @NotBlank(message = "Le PIN est obligatoire !")
    @Pattern(regexp = "^[0-9]{4}$", message = "Le PIN doit contenir exactement 4 chiffres !")
    private String pin;
}
