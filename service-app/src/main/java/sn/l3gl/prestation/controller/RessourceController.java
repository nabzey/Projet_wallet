package sn.l3gl.prestation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.l3gl.prestation.dto.AffecterRessourceRequest;
import sn.l3gl.prestation.dto.RessourceResponse;
import sn.l3gl.prestation.wrapper.RessourceWrapper;

@RestController
@RequestMapping("/api/prestations/{prestationId}/ressources")
@RequiredArgsConstructor
@Tag(name = "Ressources", description = "Affectation de ressources (personnes) à une prestation.")
public class RessourceController {

    private final RessourceWrapper ressourceWrapper;

    @Operation(summary = "Affecter une ressource à la prestation")
    @PostMapping
    public RessourceResponse affecter(@PathVariable Long prestationId,
                                       @Valid @RequestBody AffecterRessourceRequest request) {
        return ressourceWrapper.affecter(prestationId, request);
    }
}
