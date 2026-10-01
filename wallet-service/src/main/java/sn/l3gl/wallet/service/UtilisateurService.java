package sn.l3gl.wallet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sn.l3gl.wallet.exception.UtilisateurIntrouvableException;
import sn.l3gl.wallet.model.StatutUtilisateur;
import sn.l3gl.wallet.model.Utilisateur;
import sn.l3gl.wallet.repository.UtilisateurRepository;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;

    public Utilisateur findByTelephoneOuThrow(String telephone) {
        return utilisateurRepository.findByTelephone(telephone)
                .orElseThrow(UtilisateurIntrouvableException::new);
    }

    public Utilisateur findByIdOuThrow(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(UtilisateurIntrouvableException::new);
    }

    public Utilisateur creerSiAbsent(String telephone) {
        return utilisateurRepository.findByTelephone(telephone)
                .orElseGet(() -> {
                    Utilisateur utilisateur = new Utilisateur();
                    utilisateur.setTelephone(telephone);
                    utilisateur.setStatut(StatutUtilisateur.EN_ATTENTE_PIN);
                    return utilisateurRepository.save(utilisateur);
                });
    }

    public void definirPin(Utilisateur utilisateur, String pinHash) {
        utilisateur.setPinHash(pinHash);
        utilisateur.setStatut(StatutUtilisateur.ACTIF);
        utilisateurRepository.save(utilisateur);
    }
}
