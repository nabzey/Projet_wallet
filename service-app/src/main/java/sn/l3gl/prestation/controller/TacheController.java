package sn.l3gl.prestation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sn.l3gl.prestation.dto.CreateTacheRequest;
import sn.l3gl.prestation.dto.TacheResponse;
import sn.l3gl.prestation.wrapper.TacheWrapper;

import java.util.List;

@RestController
@RequestMapping("/api/ressources/{ressourceId}/taches")
@RequiredArgsConstructor
@Tag(name = "Tâches", description = "Tâches créées et affectées à une ressource, avec délai de réalisation et statut.")
public class TacheController {

    private final TacheWrapper tacheWrapper;

    @Operation(summary = "Créer une tâche pour une ressource")
    @PostMapping
    public TacheResponse creer(@PathVariable Long ressourceId, @Valid @RequestBody CreateTacheRequest request) {
        return tacheWrapper.creer(ressourceId, request);
    }

    @Operation(summary = "Lister les tâches d'une ressource")
    @GetMapping
    public List<TacheResponse> lister(@PathVariable Long ressourceId) {
        return tacheWrapper.listerParRessource(ressourceId);
    }
}
