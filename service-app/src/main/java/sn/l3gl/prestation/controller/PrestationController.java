package sn.l3gl.prestation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sn.l3gl.prestation.dto.CreatePrestationRequest;
import sn.l3gl.prestation.dto.PayerPrestationRequest;
import sn.l3gl.prestation.dto.PrestationResponse;
import sn.l3gl.prestation.wrapper.PrestationWrapper;

import java.util.List;

@RestController
@RequestMapping("/api/prestations")
@RequiredArgsConstructor
@Tag(name = "Prestations", description = "Cycle de vie d'une demande de prestation. Nécessite le JWT émis par wallet-service (bouton Authorize).")
public class PrestationController {

    private final PrestationWrapper prestationWrapper;

    @Operation(summary = "Créer une demande de prestation", description = "Statut initial : EN_ATTENTE_PAIEMENT.")
    @PostMapping
    public PrestationResponse creer(@AuthenticationPrincipal Long responsableId,
                                     @Valid @RequestBody CreatePrestationRequest request) {
        return prestationWrapper.creer(responsableId, request);
    }

    @Operation(summary = "Payer la prestation", description = "Appelle wallet-service de façon synchrone (débit) ; la confirmation PAYEE arrive ensuite de façon asynchrone via Kafka.")
    @PostMapping("/{id}/payer")
    public PrestationResponse payer(@RequestHeader("Authorization") String bearerToken,
                                     @PathVariable Long id,
                                     @Valid @RequestBody PayerPrestationRequest request) {
        return prestationWrapper.payer(bearerToken, id, request);
    }

    @Operation(summary = "Consulter une prestation")
    @GetMapping("/{id}")
    public PrestationResponse consulter(@PathVariable Long id) {
        return prestationWrapper.consulter(id);
    }

    @Operation(summary = "Lister mes prestations")
    @GetMapping("/moi")
    public List<PrestationResponse> mesPrestations(@AuthenticationPrincipal Long responsableId) {
        return prestationWrapper.mesPrestations(responsableId);
    }
}
