package sn.l3gl.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CompteResponse {
    private String numero;
    private long solde;
}
