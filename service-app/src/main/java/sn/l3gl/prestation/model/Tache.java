package sn.l3gl.prestation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "tache")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false)
    private LocalDate delaiRealisation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutTache statut = StatutTache.A_FAIRE;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ressource_id")
    private Ressource ressource;
}
