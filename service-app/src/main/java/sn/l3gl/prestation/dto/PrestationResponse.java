package sn.l3gl.prestation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrestationResponse {
    private Long id;
    private String titre;
    private String description;
    private long montant;
    private String statut;
}
