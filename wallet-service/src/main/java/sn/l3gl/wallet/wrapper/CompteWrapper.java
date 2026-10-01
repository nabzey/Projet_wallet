package sn.l3gl.wallet.wrapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sn.l3gl.wallet.dto.CompteResponse;
import sn.l3gl.wallet.dto.TransactionResponse;
import sn.l3gl.wallet.mapper.CompteMapper;
import sn.l3gl.wallet.mapper.TransactionMapper;
import sn.l3gl.wallet.model.Compte;
import sn.l3gl.wallet.service.CompteService;
import sn.l3gl.wallet.service.TransactionService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CompteWrapper {

    private final CompteService compteService;
    private final TransactionService transactionService;
    private final CompteMapper compteMapper;
    private final TransactionMapper transactionMapper;

    public CompteResponse consulter(Long utilisateurId) {
        Compte compte = compteService.findByUtilisateurIdOuThrow(utilisateurId);
        return compteMapper.toResponse(compte);
    }

    public List<TransactionResponse> historique(Long utilisateurId) {
        Compte compte = compteService.findByUtilisateurIdOuThrow(utilisateurId);
        return transactionService.historique(compte.getId()).stream()
                .map(t -> transactionMapper.toResponse(t, compte.getSolde()))
                .toList();
    }
}
