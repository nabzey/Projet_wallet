package sn.l3gl.prestation.wrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import sn.l3gl.prestation.dto.PaiementEffectueEvent;
import sn.l3gl.prestation.service.PrestationService;

/**
 * Réagit à l'event paiement-effectue publié par wallet-service (saga chorégraphiée) :
 * confirme la prestation en cas de succès, la repasse en ECHEC_PAIEMENT sinon
 * (l'utilisateur pourra retenter le paiement plus tard - compensation).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaiementListener {

    // Instancié directement (pas injecté) : Spring Boot 4 auto-configure un bean
    // tools.jackson.databind.ObjectMapper (Jackson 3), différent du classique
    // com.fasterxml.jackson.databind.ObjectMapper utilisé ici.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PrestationService prestationService;

    @KafkaListener(topics = "${kafka-topics.paiement-effectue}")
    public void onPaiementEffectue(String message, Acknowledgment acknowledgment) throws Exception {
        PaiementEffectueEvent event = OBJECT_MAPPER.readValue(message, PaiementEffectueEvent.class);

        if ("SUCCESS".equals(event.getStatut())) {
            prestationService.marquerPayee(event.getDemandeId());
        } else {
            log.warn("Paiement échoué pour la demande {} : {}", event.getDemandeId(), event.getMessage());
            prestationService.marquerEchecPaiement(event.getDemandeId());
        }

        acknowledgment.acknowledge();
    }
}
