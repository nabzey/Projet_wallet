package sn.l3gl.wallet.securite;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sn.l3gl.wallet.exception.OtpExpireException;
import sn.l3gl.wallet.exception.OtpInvalideException;
import sn.l3gl.wallet.exception.OtpTentativesDepasseesException;
import sn.l3gl.wallet.model.Otp;
import sn.l3gl.wallet.repository.OtpRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * OTP simulé : le code est loggé au lieu d'être envoyé par SMS,
 * conformément au cahier des charges ("code OTP simulé dans le cadre du projet").
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpRepository otpRepository;

    @Value("${app.otp.expiration-minutes}")
    private int expirationMinutes;

    @Value("${app.otp.max-tentatives}")
    private int maxTentatives;

    public void genererEtEnvoyer(String telephone) {
        String code = String.valueOf(1000 + RANDOM.nextInt(9000));

        Otp otp = new Otp();
        otp.setTelephone(telephone);
        otp.setCode(code);
        otp.setDateExpiration(LocalDateTime.now().plusMinutes(expirationMinutes));
        otpRepository.save(otp);

        // Simulation d'envoi : dans un vrai système, on appellerait un fournisseur SMS ici.
        System.out.println("[OTP simulé] Code envoyé à " + telephone + " : " + code);
    }

    public void verifier(String telephone, String codeSaisi) {
        Otp otp = otpRepository.findFirstByTelephoneAndUtiliseFalseOrderByIdDesc(telephone)
                .orElseThrow(OtpInvalideException::new);

        if (otp.getNombreTentatives() >= maxTentatives) {
            throw new OtpTentativesDepasseesException();
        }

        if (otp.getDateExpiration().isBefore(LocalDateTime.now())) {
            throw new OtpExpireException();
        }

        if (!otp.getCode().equals(codeSaisi)) {
            otp.setNombreTentatives(otp.getNombreTentatives() + 1);
            otpRepository.save(otp);
            throw new OtpInvalideException();
        }

        otp.setUtilise(true);
        otpRepository.save(otp);
    }
}
