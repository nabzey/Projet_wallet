package sn.l3gl.prestation.exception;

import org.springframework.http.HttpStatus;

public class PaiementInitiationException extends BusinessException {
    public PaiementInitiationException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }
}
