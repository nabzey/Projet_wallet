package sn.l3gl.wallet.wrapper;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.l3gl.wallet.dto.*;
import sn.l3gl.wallet.exception.PinInvalideException;
import sn.l3gl.wallet.helper.TransactionHelper;
import sn.l3gl.wallet.mapper.TransactionMapper;
import sn.l3gl.wallet.model.Compte;
import sn.l3gl.wallet.model.Transaction;
import sn.l3gl.wallet.model.TypeTransaction;
import sn.l3gl.wallet.model.Utilisateur;
import sn.l3gl.wallet.securite.PinService;
import sn.l3gl.wallet.service.CompteService;
import sn.l3gl.wallet.service.OutboxService;
import sn.l3gl.wallet.service.TransactionService;
import sn.l3gl.wallet.service.UtilisateurService;

/**
 * Orchestre les 3 opérations principales du wallet : dépôt, retrait, paiement.
 * Chaque méthode suit le même enchaînement : vérifier les préconditions,
 * débiter/créditer de façon atomique, enregistrer la transaction, notifier si besoin.
 */
@Component
@RequiredArgsConstructor
public class PaiementWrapper {

    private final CompteService compteService;
    private final TransactionService transactionService;
    private final UtilisateurService utilisateurService;
    private final PinService pinService;
    private final TransactionHelper transactionHelper;
    private final TransactionMapper transactionMapper;
    private final OutboxService outboxService;

    @Value("${kafka-topics.paiement-effectue}")
    private String topicPaiementEffectue;

    @Transactional
    public TransactionResponse deposer(Long utilisateurId, DepotRequest request) {
        Compte compte = compteService.findByUtilisateurIdOuThrow(utilisateurId);

        compteService.crediter(compte.getId(), request.getMontant());
        Transaction transaction = transactionHelper.construire(compte, request.getMontant(), TypeTransaction.DEPOT, null);
        transactionService.save(transaction);

        return transactionMapper.toResponse(transaction, compteService.soldeActuel(compte.getId()));
    }

    @Transactional
    public TransactionResponse retirer(Long utilisateurId, RetraitRequest request) {
        Compte compte = compteService.findByUtilisateurIdOuThrow(utilisateurId);
        verifierPin(utilisateurId, request.getPin());

        compteService.debiter(compte.getId(), request.getMontant());
        Transaction transaction = transactionHelper.construire(compte, request.getMontant(), TypeTransaction.RETRAIT, null);
        transactionService.save(transaction);

        return transactionMapper.toResponse(transaction, compteService.soldeActuel(compte.getId()));
    }

    /**
     * Paiement déclenché par service-app. Idempotent (demandeId) et notifie
     * le résultat via Outbox + Kafka pour que service-app mette à jour le statut
     * de la prestation (saga chorégraphiée, compensation gérée côté service-app).
     */
    @Transactional
    public TransactionResponse payer(Long utilisateurId, PaiementRequest request) {
        transactionService.verifierNonDejaTraite(request.getDemandeId());

        Compte compte = compteService.findByUtilisateurIdOuThrow(utilisateurId);
        verifierPin(utilisateurId, request.getPin());

        try {
            compteService.debiter(compte.getId(), request.getMontant());
        } catch (RuntimeException e) {
            outboxService.enregistrer(topicPaiementEffectue, request.getDemandeId(),
                    new PaiementEffectueEvent(request.getDemandeId(), "ECHEC", e.getMessage()));
            throw e;
        }

        Transaction transaction = transactionHelper.construire(
                compte, request.getMontant(), TypeTransaction.PAIEMENT, request.getDemandeId());
        transactionService.save(transaction);

        outboxService.enregistrer(topicPaiementEffectue, request.getDemandeId(),
                new PaiementEffectueEvent(request.getDemandeId(), "SUCCESS", "Paiement effectué"));

        return transactionMapper.toResponse(transaction, compteService.soldeActuel(compte.getId()));
    }

    private void verifierPin(Long utilisateurId, String pin) {
        Utilisateur utilisateur = utilisateurService.findByIdOuThrow(utilisateurId);
        if (!pinService.verifier(pin, utilisateur.getPinHash())) {
            throw new PinInvalideException();
        }
    }
}
