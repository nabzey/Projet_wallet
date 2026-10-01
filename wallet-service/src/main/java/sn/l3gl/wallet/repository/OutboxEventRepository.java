package sn.l3gl.wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.l3gl.wallet.model.OutboxEvent;
import sn.l3gl.wallet.model.StatutOutbox;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop50ByStatutOrderByIdAsc(StatutOutbox statut);
}
