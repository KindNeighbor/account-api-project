package project2.Account.domain;

import lombok.*;
import project2.Account.type.TransactionResultType;
import project2.Account.type.TransactionType;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Transaction extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    private TransactionResultType transactionResultType;

    @ManyToOne
    private Account account;
    private Long amount;
    private Long balanceSnapShot;

    private String transactionId;
    private LocalDateTime transactedAt;


}
