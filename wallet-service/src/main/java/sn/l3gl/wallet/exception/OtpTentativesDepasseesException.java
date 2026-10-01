package sn.l3gl.wallet.exception;

import org.springframework.http.HttpStatus;

public class OtpTentativesDepasseesException extends BusinessException {
    public OtpTentativesDepasseesException() {
        super("Nombre de tentatives dépassé, veuillez demander un nouveau code.", HttpStatus.TOO_MANY_REQUESTS);
    }
}
