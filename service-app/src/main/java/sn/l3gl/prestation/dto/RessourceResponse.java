package sn.l3gl.prestation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RessourceResponse {
    private Long id;
    private String nom;
    private String specialite;
}
