package sn.l3gl.wallet.exception;

import org.springframework.http.HttpStatus;

public class PinInvalideException extends BusinessException {
    public PinInvalideException() {
        super("Le code PIN est invalide.", HttpStatus.UNAUTHORIZED);
    }
}
