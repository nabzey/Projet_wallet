package sn.l3gl.prestation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateTacheRequest {
    @NotBlank(message = "Le libellé est obligatoire !")
    private String libelle;

    @NotNull(message = "Le délai de réalisation est obligatoire !")
    @Future(message = "Le délai doit être dans le futur !")
    private LocalDate delaiRealisation;
}
