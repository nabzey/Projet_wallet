package sn.l3gl.prestation.helper;

import org.springframework.stereotype.Component;
import sn.l3gl.prestation.model.Prestation;
import sn.l3gl.prestation.model.StatutPrestation;

/**
 * Calculs et vérifications métier purs, sans accès DB.
 */
@Component
public class PrestationHelper {

    public boolean estPayable(Prestation prestation) {
        return prestation.getStatut() == StatutPrestation.EN_ATTENTE_PAIEMENT
                || prestation.getStatut() == StatutPrestation.ECHEC_PAIEMENT;
    }
}
