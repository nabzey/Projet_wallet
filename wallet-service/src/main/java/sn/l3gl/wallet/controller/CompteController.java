package sn.l3gl.wallet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.l3gl.wallet.dto.CompteResponse;
import sn.l3gl.wallet.dto.TransactionResponse;
import sn.l3gl.wallet.wrapper.CompteWrapper;

import java.util.List;

@RestController
@RequestMapping("/api/comptes")
@RequiredArgsConstructor
@Tag(name = "Compte", description = "Consultation du compte et de l'historique. Nécessite le JWT (bouton Authorize).")
public class CompteController {

    private final CompteWrapper compteWrapper;

    @Operation(summary = "Consulter mon compte")
    @GetMapping("/moi")
    public CompteResponse monCompte(@AuthenticationPrincipal Long utilisateurId) {
        return compteWrapper.consulter(utilisateurId);
    }

    @Operation(summary = "Consulter l'historique des transactions")
    @GetMapping("/moi/transactions")
    public List<TransactionResponse> monHistorique(@AuthenticationPrincipal Long utilisateurId) {
        return compteWrapper.historique(utilisateurId);
    }
}
