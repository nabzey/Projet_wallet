package sn.l3gl.prestation.wrapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.l3gl.prestation.dto.CreateTacheRequest;
import sn.l3gl.prestation.dto.TacheResponse;
import sn.l3gl.prestation.mapper.TacheMapper;
import sn.l3gl.prestation.model.Ressource;
import sn.l3gl.prestation.model.Tache;
import sn.l3gl.prestation.service.RessourceService;
import sn.l3gl.prestation.service.TacheService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TacheWrapper {

    private final RessourceService ressourceService;
    private final TacheService tacheService;
    private final TacheMapper tacheMapper;

    @Transactional
    public TacheResponse creer(Long ressourceId, CreateTacheRequest request) {
        Ressource ressource = ressourceService.findByIdOuThrow(ressourceId);

        Tache tache = new Tache();
        tache.setLibelle(request.getLibelle());
        tache.setDelaiRealisation(request.getDelaiRealisation());
        tache.setRessource(ressource);

        return tacheMapper.toResponse(tacheService.save(tache));
    }

    public List<TacheResponse> listerParRessource(Long ressourceId) {
        return tacheService.findByRessource(ressourceId).stream()
                .map(tacheMapper::toResponse)
                .toList();
    }
}
