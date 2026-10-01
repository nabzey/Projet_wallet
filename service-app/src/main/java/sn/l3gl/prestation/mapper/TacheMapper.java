package sn.l3gl.prestation.mapper;

import org.springframework.stereotype.Component;
import sn.l3gl.prestation.dto.TacheResponse;
import sn.l3gl.prestation.model.Tache;

@Component
public class TacheMapper {

    public TacheResponse toResponse(Tache tache) {
        return new TacheResponse(
                tache.getId(),
                tache.getLibelle(),
                tache.getDelaiRealisation(),
                tache.getStatut().name()
        );
    }
}
