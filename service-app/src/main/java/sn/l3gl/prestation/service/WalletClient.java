package sn.l3gl.prestation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import sn.l3gl.prestation.dto.PaiementRequestWallet;
import sn.l3gl.prestation.exception.PaiementInitiationException;

/**
 * Appel HTTP synchrone vers wallet-service pour initier un paiement.
 * Le JWT de l'utilisateur appelant est transmis tel quel : c'est wallet-service
 * qui reste seul responsable de l'authentification et de la vérification du PIN.
 */
@Service
@Slf4j
public class WalletClient {

    private final RestClient restClient;

    public WalletClient(@Value("${app.wallet.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void initierPaiement(String bearerToken, PaiementRequestWallet request) {
        try {
            restClient.post()
                    .uri("/api/transactions/paiement")
                    .header("Authorization", bearerToken)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("Paiement refusé par wallet-service pour la demande {} : {}",
                    request.getDemandeId(), e.getResponseBodyAsString());
            throw new PaiementInitiationException(e.getResponseBodyAsString());
        }
    }
}
