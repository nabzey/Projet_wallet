package sn.l3gl.wallet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sn.l3gl.wallet.exception.CompteIntrouvableException;
import sn.l3gl.wallet.exception.SoldeInsuffisantException;
import sn.l3gl.wallet.model.Compte;
import sn.l3gl.wallet.model.Utilisateur;
import sn.l3gl.wallet.repository.CompteRepository;

@Service
@RequiredArgsConstructor
public class CompteService {

    private final CompteRepository compteRepository;

    public Compte creer(Utilisateur utilisateur, String numero) {
        Compte compte = new Compte();
        compte.setNumero(numero);
        compte.setUtilisateur(utilisateur);
        return compteRepository.save(compte);
    }

    public Compte findByUtilisateurIdOuThrow(Long utilisateurId) {
        return compteRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(CompteIntrouvableException::new);
    }

    public Compte findByNumeroOuThrow(String numero) {
        return compteRepository.findByNumero(numero)
                .orElseThrow(CompteIntrouvableException::new);
    }

    /**
     * Débit atomique : lève SoldeInsuffisantException si la requête UPDATE
     * n'affecte aucune ligne (solde insuffisant), sans jamais lire le solde
     * en mémoire au préalable — élimine la race condition trouvée dans banque-api.
     */
    public void debiter(Long compteId, long montant) {
        int lignes = compteRepository.debiterSiSoldeSuffisant(compteId, montant);
        if (lignes == 0) {
            throw new SoldeInsuffisantException();
        }
    }

    public void crediter(Long compteId, long montant) {
        compteRepository.crediter(compteId, montant);
    }

    public long soldeActuel(Long compteId) {
        return compteRepository.findById(compteId)
                .orElseThrow(CompteIntrouvableException::new)
                .getSolde();
    }
}
