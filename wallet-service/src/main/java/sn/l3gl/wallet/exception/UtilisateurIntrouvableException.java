package sn.l3gl.wallet.exception;

import org.springframework.http.HttpStatus;

public class UtilisateurIntrouvableException extends BusinessException {
    public UtilisateurIntrouvableException() {
        super("Utilisateur introuvable.", HttpStatus.NOT_FOUND);
    }
}
