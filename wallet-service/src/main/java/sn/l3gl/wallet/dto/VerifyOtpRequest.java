package sn.l3gl.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank(message = "Le numéro de téléphone est obligatoire !")
    private String telephone;

    @NotBlank(message = "Le code OTP est obligatoire !")
    private String code;
}
