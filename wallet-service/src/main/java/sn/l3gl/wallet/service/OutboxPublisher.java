package sn.l3gl.wallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sn.l3gl.wallet.model.OutboxEvent;
import sn.l3gl.wallet.model.StatutOutbox;
import sn.l3gl.wallet.repository.OutboxEventRepository;

import java.util.List;

/**
 * Scheduler qui relit périodiquement la table outbox_event et publie sur Kafka.
 * Sépare l'écriture métier (transactionnelle, fiable) de la publication réseau
 * (potentiellement instable) — Outbox Pattern.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.fixed-delay-ms}")
    @org.springframework.transaction.annotation.Transactional
    public void publier() {
        List<OutboxEvent> evenements = outboxEventRepository.findTop50ByStatutOrderByIdAsc(StatutOutbox.PENDING);

        for (OutboxEvent event : evenements) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getCle(), event.getPayload()).get();
                event.setStatut(StatutOutbox.PUBLISHED);
            } catch (Exception e) {
                log.error("Échec de publication de l'event outbox {} : {}", event.getId(), e.getMessage());
                event.setStatut(StatutOutbox.PENDING);
            }
            outboxEventRepository.save(event);
        }
    }
}
