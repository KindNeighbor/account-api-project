package project2.Account.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project2.Account.domain.Account;
import project2.Account.domain.AccountUser;
import project2.Account.domain.Transaction;
import project2.Account.dto.TransactionDto;
import project2.Account.execption.AccountException;
import project2.Account.repository.AccountRepository;
import project2.Account.repository.AccountUserRepository;
import project2.Account.repository.TransactionRepository;
import project2.Account.type.AccountStatus;
import project2.Account.type.ErrorCode;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static project2.Account.type.AccountStatus.IN_USE;
import static project2.Account.type.TransactionResultType.F;
import static project2.Account.type.TransactionResultType.S;
import static project2.Account.type.TransactionType.CANCEL;
import static project2.Account.type.TransactionType.USE;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    public static final Long USE_AMOUNT = 200L;
    public static final Long CANCEL_AMOUNT = 200L;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void successUseBalance() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Choi").build();
        user.setId(12L);
        Account account = Account.builder()
                        .accountUser(user)
                        .accountStatus(IN_USE)
                        .balance(10000L)
                        .accountNumber("1000000012").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapShot(9000L)
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        
        //when
        TransactionDto transactionDto = transactionService.useBalance(1L, "1000000000", USE_AMOUNT);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        assertEquals(9800L, captor.getValue().getBalanceSnapShot());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(9000L, transactionDto.getBalanceSnapShot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 유저 없음 - 잔액 사용 실패")
    void useBalance_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 실패")
    void useBalance_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Choi").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 - 잔액 사용 실패")
    void useBalance_userUnMatch() {
        //given
        AccountUser choi = AccountUser.builder()
                .name("Choi").build();
        choi.setId(12L);
        AccountUser kim = AccountUser.builder()
                .name("Kim").build();
        choi.setId(13L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(choi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(kim)
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("해지 계좌는 해지할 수 없다.")
    void useBalance_alreadyUnregistered() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Choi").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 금액이 잔액보다 큰 경우")
    void ㅕseBalance_exceedAmount() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Choi").build();
        user.setId(12L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(100L)
                .accountNumber("1000000012").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        //then
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
        verify(transactionRepository, times(0)).save(any());

    }

    @Test
    @DisplayName("실패 트랜잭션 저장 성공")
    void saveFailedUseTransaction() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Choi").build();
        user.setId(12L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapShot(9000L)
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        transactionService.saveFailedUseTransaction("1000000000", USE_AMOUNT);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapShot());
        assertEquals(F, captor.getValue().getTransactionResultType());
    }

    @Test
    void successCancelBalance() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Choi").build();
        user.setId(12L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapShot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(CANCEL)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(CANCEL_AMOUNT)
                        .balanceSnapShot(10000L)
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto = transactionService.cancelBalance("transactionId",
                "1000000000", CANCEL_AMOUNT);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(CANCEL_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L + CANCEL_AMOUNT, captor.getValue().getBalanceSnapShot());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL, transactionDto.getTransactionType());
        assertEquals(10000L, transactionDto.getBalanceSnapShot());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 취소 실패")
    void cancelBalance_AccountNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1000000000", 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("원 사용 거래 없음 - 잔액 사용 취소 실패")
    void cancelBalance_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1000000000", 1000L));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래와 계좌가 매칭실패 - 잔액 사용 취소 실패")
    void cancelBalance_TransactionAccountUnMatch() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Choi").build();
        user.setId(12L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);
        Account accountNotUse = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000013").build();
        accountNotUse.setId(2L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(CANCEL)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapShot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountNotUse));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1000000000", CANCEL_AMOUNT));

        //then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래금액과 취소금액이 다름 - 잔액 사용 취소 실패")
    void cancelBalance_CancelMustFully() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Choi").build();
        user.setId(12L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(CANCEL)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT + 1000L)
                .balanceSnapShot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1000000000", CANCEL_AMOUNT));

        //then
        assertEquals(ErrorCode.CANCEL_MUST_FULLY, exception.getErrorCode());
    }

    @Test
    @DisplayName("취소는 1년까지만 가능 - 잔액 사용 취소 실패")
    void cancelBalance_TooOldOrder() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Choi").build();
        user.setId(12L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(CANCEL)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusNanos(1))
                .amount(CANCEL_AMOUNT)
                .balanceSnapShot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId",
                        "1000000000", CANCEL_AMOUNT));

        //then
        assertEquals(ErrorCode.TOO_OLD_ORDER_TO_CANCEL, exception.getErrorCode());
    }

    @Test
    void successQueryTransaction() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Choi").build();
        user.setId(12L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusNanos(1))
                .amount(CANCEL_AMOUNT)
                .balanceSnapShot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        //when
        TransactionDto transactionDto = transactionService.queryTransaction("trxId");

        //then
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
        assertEquals("transactionId", transactionDto.getTransactionId());
    }

    @Test
    @DisplayName("원거래 없음 - 잔액 사용 취소 실패")
    void queryTransaction_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("transactionId"));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }
}