package sn.l3gl.wallet.exception;

import org.springframework.http.HttpStatus;

public class CompteIntrouvableException extends BusinessException {
    public CompteIntrouvableException() {
        super("Compte introuvable.", HttpStatus.NOT_FOUND);
    }
}
