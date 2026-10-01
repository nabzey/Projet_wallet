package sn.l3gl.wallet.securite;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PinService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hacher(String pin) {
        return encoder.encode(pin);
    }

    public boolean verifier(String pinSaisi, String pinHash) {
        return encoder.matches(pinSaisi, pinHash);
    }
}
