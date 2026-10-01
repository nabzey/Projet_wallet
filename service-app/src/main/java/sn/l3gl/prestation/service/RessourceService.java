package sn.l3gl.prestation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sn.l3gl.prestation.model.Ressource;
import sn.l3gl.prestation.repository.RessourceRepository;

@Service
@RequiredArgsConstructor
public class RessourceService {

    private final RessourceRepository ressourceRepository;

    public Ressource save(Ressource ressource) {
        return ressourceRepository.save(ressource);
    }

    public Ressource findByIdOuThrow(Long id) {
        return ressourceRepository.findById(id)
                .orElseThrow(() -> new sn.l3gl.prestation.exception.BusinessException(
                        "Ressource introuvable.", org.springframework.http.HttpStatus.NOT_FOUND));
    }
}
