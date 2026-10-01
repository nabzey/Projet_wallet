package sn.l3gl.wallet.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String reference;

    @Column(nullable = false)
    private long montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeTransaction type;

    /**
     * Identifiant de la demande de service (service-app) à l'origine d'un PAIEMENT.
     * Sert de clé d'idempotence : null pour DEPOT/RETRAIT.
     */
    @Column(unique = true)
    private String demandeId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "compte_id")
    private Compte compte;

    private LocalDateTime dateTransaction = LocalDateTime.now();
}
