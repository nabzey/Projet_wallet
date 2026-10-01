package sn.l3gl.wallet.mapper;

import org.springframework.stereotype.Component;
import sn.l3gl.wallet.dto.CompteResponse;
import sn.l3gl.wallet.model.Compte;

@Component
public class CompteMapper {

    public CompteResponse toResponse(Compte compte) {
        return new CompteResponse(compte.getNumero(), compte.getSolde());
    }
}
