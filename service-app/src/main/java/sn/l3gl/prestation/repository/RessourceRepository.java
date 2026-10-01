package sn.l3gl.prestation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.l3gl.prestation.model.Ressource;

@Repository
public interface RessourceRepository extends JpaRepository<Ressource, Long> {
}
