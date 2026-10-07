package sn.l3gl.wallet.wrapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.l3gl.wallet.dto.AuthResponse;
import sn.l3gl.wallet.dto.CreatePinRequest;
import sn.l3gl.wallet.dto.RequestOtpRequest;
import sn.l3gl.wallet.dto.VerifyOtpRequest;
import sn.l3gl.wallet.helper.TransactionHelper;
import sn.l3gl.wallet.model.Compte;
import sn.l3gl.wallet.model.Utilisateur;
import sn.l3gl.wallet.securite.JwtService;
import sn.l3gl.wallet.securite.OtpService;
import sn.l3gl.wallet.securite.PinService;
import sn.l3gl.wallet.service.CompteService;
import sn.l3gl.wallet.service.UtilisateurService;

/**
 * Orchestre le flux d'authentification complet :
 * demande OTP -> vérification OTP (création utilisateur si nouveau) -> création PIN -> JWT.
 */
@Component
@RequiredArgsConstructor
public class AuthWrapper {

    private final OtpService otpService;
    private final PinService pinService;
    private final JwtService jwtService;
    private final UtilisateurService utilisateurService;
    private final CompteService compteService;
    private final TransactionHelper transactionHelper;

    public void demanderOtp(RequestOtpRequest request) {
        otpService.genererEtEnvoyer(request.getTelephone());
    }

    public String verifierOtp(VerifyOtpRequest request) {
        otpService.verifier(request.getTelephone(), request.getCode());
        utilisateurService.creerSiAbsent(request.getTelephone());
        return jwtService.genererVerificationToken(request.getTelephone());
    }

    public AuthResponse connecter(CreatePinRequest request) {
        Utilisateur utilisateur = utilisateurService.findByTelephoneOuThrow(request.getTelephone());
        if (utilisateur.getPinHash() == null || !pinService.verifier(request.getPin(), utilisateur.getPinHash())) {
            throw new sn.l3gl.wallet.exception.PinInvalideException();
        }
        Compte compte = compteService.findByUtilisateurIdOuThrow(utilisateur.getId());
        return new AuthResponse(jwtService.genererToken(utilisateur.getId(), utilisateur.getTelephone()), compte.getNumero());
    }

    @Transactional
    public AuthResponse creerPin(CreatePinRequest request) {
        if (!jwtService.verificationValide(request.getVerificationToken(), request.getTelephone())) {
            throw new sn.l3gl.wallet.exception.BusinessException("Vérifiez votre code OTP avant de créer le PIN.");
        }
        Utilisateur utilisateur = utilisateurService.findByTelephoneOuThrow(request.getTelephone());

        if (utilisateur.getPinHash() != null) {
            throw new sn.l3gl.wallet.exception.BusinessException("Ce compte existe déjà. Utilisez la connexion avec votre PIN.");
        }
        String pinHash = pinService.hacher(request.getPin());
        utilisateurService.definirPin(utilisateur, pinHash);

        Compte compte = compteService.creer(utilisateur, transactionHelper.genererNumeroCompte());

        String token = jwtService.genererToken(utilisateur.getId(), utilisateur.getTelephone());
        return new AuthResponse(token, compte.getNumero());
    }
}
