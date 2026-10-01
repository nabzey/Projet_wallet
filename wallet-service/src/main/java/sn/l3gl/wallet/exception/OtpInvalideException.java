package sn.l3gl.wallet.exception;

public class OtpInvalideException extends BusinessException {
    public OtpInvalideException() {
        super("Le code OTP est invalide.");
    }
}
