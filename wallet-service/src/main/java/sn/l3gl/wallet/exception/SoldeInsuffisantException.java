package sn.l3gl.wallet.exception;

public class SoldeInsuffisantException extends BusinessException {
    public SoldeInsuffisantException() {
        super("Solde insuffisant pour effectuer cette opération.");
    }
}
