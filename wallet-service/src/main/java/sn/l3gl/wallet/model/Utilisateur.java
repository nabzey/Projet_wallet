package sn.l3gl.wallet.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "utilisateur")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String telephone;

    private String email;

    private String pinHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutUtilisateur statut = StatutUtilisateur.EN_ATTENTE_PIN;

    private LocalDateTime dateCreation = LocalDateTime.now();
}
