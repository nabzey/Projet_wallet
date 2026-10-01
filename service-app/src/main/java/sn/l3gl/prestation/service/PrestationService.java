package sn.l3gl.prestation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sn.l3gl.prestation.exception.PrestationIntrouvableException;
import sn.l3gl.prestation.exception.TransitionStatutInvalideException;
import sn.l3gl.prestation.model.Prestation;
import sn.l3gl.prestation.model.StatutPrestation;
import sn.l3gl.prestation.repository.PrestationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrestationService {

    private final PrestationRepository prestationRepository;

    public Prestation save(Prestation prestation) {
        return prestationRepository.save(prestation);
    }

    public Prestation findByIdOuThrow(Long id) {
        return prestationRepository.findById(id)
                .orElseThrow(PrestationIntrouvableException::new);
    }

    public List<Prestation> findByResponsable(Long responsableId) {
        return prestationRepository.findByResponsableId(responsableId);
    }

    public Prestation findByDemandeIdOuThrow(String demandeId) {
        Long id = extraireId(demandeId);
        return findByIdOuThrow(id);
    }

    public void marquerPayee(String demandeId) {
        Prestation prestation = findByDemandeIdOuThrow(demandeId);
        prestation.setStatut(StatutPrestation.PAYEE);
        prestationRepository.save(prestation);
    }

    public void marquerEchecPaiement(String demandeId) {
        Prestation prestation = findByDemandeIdOuThrow(demandeId);
        prestation.setStatut(StatutPrestation.ECHEC_PAIEMENT);
        prestationRepository.save(prestation);
    }

    public void demarrer(Prestation prestation) {
        if (prestation.getStatut() != StatutPrestation.PAYEE) {
            throw new TransitionStatutInvalideException("Seule une prestation PAYEE peut démarrer.");
        }
        prestation.setStatut(StatutPrestation.EN_COURS);
        prestationRepository.save(prestation);
    }

    private Long extraireId(String demandeId) {
        try {
            return Long.parseLong(demandeId.replace("PRESTATION-", ""));
        } catch (NumberFormatException e) {
            throw new PrestationIntrouvableException();
        }
    }
}
