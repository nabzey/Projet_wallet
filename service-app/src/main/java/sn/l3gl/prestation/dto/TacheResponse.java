package sn.l3gl.prestation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class TacheResponse {
    private Long id;
    private String libelle;
    private LocalDate delaiRealisation;
    private String statut;
}
