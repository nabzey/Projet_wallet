package sn.l3gl.wallet.exception;

import org.springframework.http.HttpStatus;

public class PaiementDejaTraiteException extends BusinessException {
    public PaiementDejaTraiteException() {
        super("Cette demande de paiement a déjà été traitée.", HttpStatus.CONFLICT);
    }
}
