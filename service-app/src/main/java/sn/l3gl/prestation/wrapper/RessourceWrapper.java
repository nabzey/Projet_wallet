package sn.l3gl.prestation.wrapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.l3gl.prestation.dto.AffecterRessourceRequest;
import sn.l3gl.prestation.dto.RessourceResponse;
import sn.l3gl.prestation.mapper.RessourceMapper;
import sn.l3gl.prestation.model.Prestation;
import sn.l3gl.prestation.model.Ressource;
import sn.l3gl.prestation.service.PrestationService;
import sn.l3gl.prestation.service.RessourceService;

@Component
@RequiredArgsConstructor
public class RessourceWrapper {

    private final PrestationService prestationService;
    private final RessourceService ressourceService;
    private final RessourceMapper ressourceMapper;

    @Transactional
    public RessourceResponse affecter(Long prestationId, AffecterRessourceRequest request) {
        Prestation prestation = prestationService.findByIdOuThrow(prestationId);

        Ressource ressource = new Ressource();
        ressource.setNom(request.getNom());
        ressource.setSpecialite(request.getSpecialite());
        ressource.setPrestation(prestation);

        return ressourceMapper.toResponse(ressourceService.save(ressource));
    }
}
