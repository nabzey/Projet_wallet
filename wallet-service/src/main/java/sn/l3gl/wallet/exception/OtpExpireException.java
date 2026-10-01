package sn.l3gl.wallet.exception;

public class OtpExpireException extends BusinessException {
    public OtpExpireException() {
        super("Le code OTP a expiré, veuillez en demander un nouveau.");
    }
}
