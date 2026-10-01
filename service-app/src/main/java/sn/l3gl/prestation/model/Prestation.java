package sn.l3gl.prestation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prestation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Prestation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    private String description;

    @Column(nullable = false)
    private long montant;

    /**
     * Id du compte wallet (chez wallet-service) du responsable, jamais de données
     * financières dupliquées ici — cf. principe de séparation des données par service.
     */
    @Column(nullable = false)
    private Long responsableId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPrestation statut = StatutPrestation.EN_ATTENTE_PAIEMENT;

    @OneToMany(mappedBy = "prestation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ressource> ressources = new ArrayList<>();

    private LocalDateTime dateCreation = LocalDateTime.now();

    public String demandeId() {
        return "PRESTATION-" + id;
    }
}
