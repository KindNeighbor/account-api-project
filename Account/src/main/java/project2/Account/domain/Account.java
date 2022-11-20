package project2.Account.domain;


import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import project2.Account.execption.AccountException;
import project2.Account.type.AccountStatus;
import project2.Account.type.ErrorCode;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Account extends BaseEntity {

    @ManyToOne
    private AccountUser accountUser;
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;

    private Long balance;

    private LocalDateTime registeredAt;
    private LocalDateTime unRegisteredAt;

    public void useBalance(Long amount) {
        if (amount > balance) {
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }

        balance -= amount;
    }

    public void cancelBalance(Long amount) {
        if (amount < 0) {
            throw new AccountException(ErrorCode.INVALID_REQUEST);
        }

        balance += amount;
    }
}
