package sn.l3gl.wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sn.l3gl.wallet.model.Compte;

import java.util.Optional;

@Repository
public interface CompteRepository extends JpaRepository<Compte, Long> {

    Optional<Compte> findByNumero(String numero);

    Optional<Compte> findByUtilisateurId(Long utilisateurId);

    Optional<Compte> findByUtilisateurTelephone(String telephone);

    /**
     * Débit atomique en base : la condition solde >= montant est vérifiée par
     * la requête elle-même, pas en mémoire. Deux débits concurrents sur le même
     * compte ne peuvent donc jamais tous les deux réussir avec un solde insuffisant
     * (contrairement à un schéma "lire le solde puis réécrire").
     * @return le nombre de lignes affectées : 0 si le solde était insuffisant.
     */
    @Modifying
    @Query("UPDATE Compte c SET c.solde = c.solde - :montant WHERE c.id = :id AND c.solde >= :montant")
    int debiterSiSoldeSuffisant(@Param("id") Long id, @Param("montant") long montant);

    @Modifying
    @Query("UPDATE Compte c SET c.solde = c.solde + :montant WHERE c.id = :id")
    int crediter(@Param("id") Long id, @Param("montant") long montant);
}
