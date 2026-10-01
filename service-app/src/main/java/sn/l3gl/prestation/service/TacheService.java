package sn.l3gl.prestation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sn.l3gl.prestation.model.Tache;
import sn.l3gl.prestation.repository.TacheRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TacheService {

    private final TacheRepository tacheRepository;

    public Tache save(Tache tache) {
        return tacheRepository.save(tache);
    }

    public List<Tache> findByRessource(Long ressourceId) {
        return tacheRepository.findByRessourceId(ressourceId);
    }
}
