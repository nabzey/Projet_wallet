package sn.l3gl.wallet.mapper;

import org.springframework.stereotype.Component;
import sn.l3gl.wallet.dto.TransactionResponse;
import sn.l3gl.wallet.model.Transaction;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction, long nouveauSolde) {
        return new TransactionResponse(
                transaction.getReference(),
                transaction.getMontant(),
                transaction.getType().name(),
                nouveauSolde,
                transaction.getDateTransaction()
        );
    }
}
