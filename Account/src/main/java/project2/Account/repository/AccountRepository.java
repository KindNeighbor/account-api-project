package project2.Account.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project2.Account.domain.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
}
