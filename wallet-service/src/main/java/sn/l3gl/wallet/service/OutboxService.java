package sn.l3gl.wallet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import sn.l3gl.wallet.model.OutboxEvent;
import sn.l3gl.wallet.repository.OutboxEventRepository;

@Service
@RequiredArgsConstructor
public class OutboxService {

    // Instancié directement (pas injecté) : Spring Boot 4 auto-configure un bean
    // tools.jackson.databind.ObjectMapper (Jackson 3), différent du classique
    // com.fasterxml.jackson.databind.ObjectMapper utilisé ici.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OutboxEventRepository outboxEventRepository;

    /**
     * Enregistre l'event dans la même transaction DB que l'opération métier
     * (appelé depuis un Wrapper déjà @Transactional) — garantit qu'un débit
     * n'est jamais persisté sans que son event de notification le soit aussi.
     */
    @SneakyThrows
    public void enregistrer(String topic, String cle, Object payload) {
        OutboxEvent event = new OutboxEvent();
        event.setTopic(topic);
        event.setCle(cle);
        event.setPayload(OBJECT_MAPPER.writeValueAsString(payload));
        outboxEventRepository.save(event);
    }
}
