package sn.l3gl.prestation.exception;

import org.springframework.http.HttpStatus;

public class PrestationIntrouvableException extends BusinessException {
    public PrestationIntrouvableException() {
        super("Prestation introuvable.", HttpStatus.NOT_FOUND);
    }
}
