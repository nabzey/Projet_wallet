package sn.l3gl.prestation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.l3gl.prestation.model.Prestation;

import java.util.List;

@Repository
public interface PrestationRepository extends JpaRepository<Prestation, Long> {
    List<Prestation> findByResponsableId(Long responsableId);
}
