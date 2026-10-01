package sn.l3gl.wallet.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Otp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String telephone;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private LocalDateTime dateExpiration;

    private int nombreTentatives = 0;

    private boolean utilise = false;
}
