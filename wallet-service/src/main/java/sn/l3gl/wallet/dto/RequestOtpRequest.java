package sn.l3gl.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RequestOtpRequest {
    @NotBlank(message = "Le numéro de téléphone est obligatoire !")
    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Format de téléphone invalide !")
    private String telephone;
}
