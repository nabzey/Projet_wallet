package sn.l3gl.prestation.mapper;

import org.springframework.stereotype.Component;
import sn.l3gl.prestation.dto.PrestationResponse;
import sn.l3gl.prestation.model.Prestation;

@Component
public class PrestationMapper {

    public PrestationResponse toResponse(Prestation prestation) {
        return new PrestationResponse(
                prestation.getId(),
                prestation.getTitre(),
                prestation.getDescription(),
                prestation.getMontant(),
                prestation.getStatut().name()
        );
    }
}
