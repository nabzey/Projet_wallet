package sn.l3gl.prestation.wrapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.l3gl.prestation.dto.CreatePrestationRequest;
import sn.l3gl.prestation.dto.PaiementRequestWallet;
import sn.l3gl.prestation.dto.PayerPrestationRequest;
import sn.l3gl.prestation.dto.PrestationResponse;
import sn.l3gl.prestation.exception.TransitionStatutInvalideException;
import sn.l3gl.prestation.helper.PrestationHelper;
import sn.l3gl.prestation.mapper.PrestationMapper;
import sn.l3gl.prestation.model.Prestation;
import sn.l3gl.prestation.service.PrestationService;
import sn.l3gl.prestation.service.WalletClient;

import java.util.List;

/**
 * Orchestre le cycle de vie d'une prestation : création, demande de paiement
 * (appel synchrone à wallet-service), consultation.
 */
@Component
@RequiredArgsConstructor
public class PrestationWrapper {

    private final PrestationService prestationService;
    private final PrestationHelper prestationHelper;
    private final PrestationMapper prestationMapper;
    private final WalletClient walletClient;

    @Transactional
    public PrestationResponse creer(Long responsableId, CreatePrestationRequest request) {
        Prestation prestation = new Prestation();
        prestation.setTitre(request.getTitre());
        prestation.setDescription(request.getDescription());
        prestation.setMontant(request.getMontant());
        prestation.setResponsableId(responsableId);

        prestation = prestationService.save(prestation);
        return prestationMapper.toResponse(prestation);
    }

    public PrestationResponse payer(String bearerToken, Long prestationId, PayerPrestationRequest request) {
        Prestation prestation = prestationService.findByIdOuThrow(prestationId);

        if (!prestationHelper.estPayable(prestation)) {
            throw new TransitionStatutInvalideException(
                    "Cette prestation ne peut pas être payée dans son état actuel : " + prestation.getStatut());
        }

        walletClient.initierPaiement(bearerToken, new PaiementRequestWallet(
                prestation.getMontant(), request.getPin(), prestation.demandeId()));

        // Confirmation définitive du statut PAYEE via le listener Kafka (paiement-effectue),
        // ce qui garantit la cohérence même si la réponse HTTP se perd après le débit.
        return prestationMapper.toResponse(prestation);
    }

    public PrestationResponse consulter(Long prestationId) {
        return prestationMapper.toResponse(prestationService.findByIdOuThrow(prestationId));
    }

    public List<PrestationResponse> mesPrestations(Long responsableId) {
        return prestationService.findByResponsable(responsableId).stream()
                .map(prestationMapper::toResponse)
                .toList();
    }
}
