package sn.l3gl.wallet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.l3gl.wallet.dto.DepotRequest;
import sn.l3gl.wallet.dto.PaiementRequest;
import sn.l3gl.wallet.dto.RetraitRequest;
import sn.l3gl.wallet.dto.TransactionResponse;
import sn.l3gl.wallet.wrapper.PaiementWrapper;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Dépôt, retrait, paiement. Nécessite le JWT (bouton Authorize).")
public class TransactionController {

    private final PaiementWrapper paiementWrapper;

    @Operation(summary = "Déposer de l'argent sur son compte")
    @PostMapping("/depot")
    public TransactionResponse deposer(@AuthenticationPrincipal Long utilisateurId,
                                        @Valid @RequestBody DepotRequest request) {
        return paiementWrapper.deposer(utilisateurId, request);
    }

    @Operation(summary = "Retirer de l'argent (PIN requis)")
    @PostMapping("/retrait")
    public TransactionResponse retirer(@AuthenticationPrincipal Long utilisateurId,
                                        @Valid @RequestBody RetraitRequest request) {
        return paiementWrapper.retirer(utilisateurId, request);
    }

    @Operation(summary = "Payer une prestation (PIN requis)", description = "Appelé par service-app avec le demandeId de la prestation. Idempotent.")
    @PostMapping("/paiement")
    public TransactionResponse payer(@AuthenticationPrincipal Long utilisateurId,
                                      @Valid @RequestBody PaiementRequest request) {
        return paiementWrapper.payer(utilisateurId, request);
    }
}
