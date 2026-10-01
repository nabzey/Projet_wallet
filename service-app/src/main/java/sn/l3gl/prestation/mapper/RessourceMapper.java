package sn.l3gl.prestation.mapper;

import org.springframework.stereotype.Component;
import sn.l3gl.prestation.dto.RessourceResponse;
import sn.l3gl.prestation.model.Ressource;

@Component
public class RessourceMapper {

    public RessourceResponse toResponse(Ressource ressource) {
        return new RessourceResponse(ressource.getId(), ressource.getNom(), ressource.getSpecialite());
    }
}
