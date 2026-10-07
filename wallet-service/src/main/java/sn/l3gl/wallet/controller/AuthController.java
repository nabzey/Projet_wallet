package sn.l3gl.wallet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.l3gl.wallet.dto.AuthResponse;
import sn.l3gl.wallet.dto.CreatePinRequest;
import sn.l3gl.wallet.dto.RequestOtpRequest;
import sn.l3gl.wallet.dto.VerifyOtpRequest;
import sn.l3gl.wallet.wrapper.AuthWrapper;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Flux OTP -> PIN -> JWT. Ces 3 endpoints sont publics (pas de token requis).")
public class AuthController {

    private final AuthWrapper authWrapper;

    @Operation(summary = "1. Demander un code OTP", description = "Le code est simulé : il s'affiche dans les logs de l'application au lieu d'être envoyé par SMS.")
    @PostMapping("/request-otp")
    public ResponseEntity<Map<String, String>> demanderOtp(@Valid @RequestBody RequestOtpRequest request) {
        authWrapper.demanderOtp(request);
        return ResponseEntity.ok(Map.of("message", "Code OTP envoyé !"));
    }

    @Operation(summary = "2. Vérifier le code OTP", description = "Crée le compte utilisateur si le numéro est nouveau. Utiliser le code affiché dans les logs.")
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifierOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String verificationToken = authWrapper.verifierOtp(request);
        return ResponseEntity.ok(Map.of("message", "OTP vérifié, veuillez définir votre PIN.", "verificationToken", verificationToken));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> connecter(@Valid @RequestBody CreatePinRequest request) {
        return ResponseEntity.ok(authWrapper.connecter(request));
    }

    @Operation(summary = "3. Créer le PIN", description = "Définit le PIN (4 chiffres), crée le compte wallet et retourne le JWT à utiliser via le bouton Authorize.")
    @PostMapping("/create-pin")
    public ResponseEntity<AuthResponse> creerPin(@Valid @RequestBody CreatePinRequest request) {
        return ResponseEntity.ok(authWrapper.creerPin(request));
    }
}
