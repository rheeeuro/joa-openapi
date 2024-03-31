package com.joa.openapi.transaction.service;

import com.joa.openapi.account.entity.Account;
import com.joa.openapi.account.errorcode.AccountErrorCode;
import com.joa.openapi.account.repository.AccountRepository;
import com.joa.openapi.bank.entity.Bank;
import com.joa.openapi.bank.errorcode.BankErrorCode;
import com.joa.openapi.bank.repository.BankRepository;
import com.joa.openapi.common.errorcode.CommonErrorCode;
import com.joa.openapi.common.exception.RestApiException;
import com.joa.openapi.common.repository.ApiRepository;
import com.joa.openapi.dummy.entity.Dummy;
import com.joa.openapi.dummy.errorcode.DummyErrorCode;
import com.joa.openapi.dummy.repository.DummyRepository;
import com.joa.openapi.product.dto.res.ProductSearchResponseDto;
import com.joa.openapi.product.entity.Product;
import com.joa.openapi.transaction.dto.req.Transaction1wonConfirmRequestDto;
import com.joa.openapi.transaction.dto.req.Transaction1wonRequestDto;
import com.joa.openapi.transaction.dto.req.TransactionDeleteRequestDto;
import com.joa.openapi.transaction.dto.req.TransactionRequestDto;
import com.joa.openapi.transaction.dto.req.TransactionSearchRequestDto;
import com.joa.openapi.transaction.dto.req.TransactionUpdateRequestDto;
import com.joa.openapi.transaction.dto.res.Transaction1wonResponseDto;
import com.joa.openapi.transaction.dto.res.TransactionResponseDto;
import com.joa.openapi.transaction.dto.res.TransactionSearchResponseDto;
import com.joa.openapi.transaction.dto.res.TransactionUpdateResponseDto;
import com.joa.openapi.transaction.entity.Fourwords;
import com.joa.openapi.transaction.entity.Transaction;
import com.joa.openapi.transaction.errorcode.TransactionErrorCode;
import com.joa.openapi.transaction.repository.TransactionRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final DummyRepository dummyRepository;
    private final ApiRepository apiRepository;
    private final BankRepository bankRepository;

    @Transactional
    public TransactionResponseDto deposit(UUID apiKey, TransactionRequestDto req) {

        String to = req.getToAccount();

        Account account = accountRepository.findById(to).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));

        Optional<Dummy> optionalDummy = Optional.ofNullable(req.getDummyId())
                .map(dummyId -> dummyRepository.findById(dummyId).orElseThrow(() -> new RestApiException(
                    DummyErrorCode.NO_DUMMY)));

        bankAuthorityValidation(apiKey, account.getBankId());
        checkPassword(account, req.getPassword());

        Long toPrevBalance = account.getBalance();

        if (req.getAmount() != null){
            account.updateBalance(account.getBalance() + req.getAmount());
        }

        Transaction transaction = Transaction.builder()
                .amount(req.getAmount())
                .depositorName(req.getDepositorName() == null ? "입금" : req.getDepositorName())
                .fromAccount(null)
                .toAccount(req.getToAccount())
                .dummy(optionalDummy.orElse(null))
                .build();

        transactionRepository.save(transaction);
        accountRepository.save(account);

        return TransactionResponseDto.toDepositDto(transaction, toPrevBalance, account.getBalance());
    }

    @Transactional
    public TransactionResponseDto withdraw(UUID apiKey, TransactionRequestDto req) {

        String from = req.getFromAccount();

        Account account = accountRepository.findById(from).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));

        Optional<Dummy> optionalDummy = Optional.ofNullable(req.getDummyId())
                .map(dummyId -> dummyRepository.findById(dummyId).orElseThrow(() -> new RestApiException(DummyErrorCode.NO_DUMMY)));

        bankAuthorityValidation(apiKey, account.getBankId());
        checkPassword(account, req.getPassword());

        if(account.getBalance() < req.getAmount())
            throw new RestApiException(TransactionErrorCode.NO_BALANCE);

        Long fromPrevBalance = account.getBalance();

        account.updateBalance(account.getBalance() - req.getAmount());

        Transaction transaction = Transaction.builder()
                .amount(req.getAmount())
                .depositorName(req.getDepositorName() == null ? "출금" : req.getDepositorName())
                .fromAccount(req.getFromAccount())
                .toAccount(null)
                .dummy(optionalDummy.orElse(null))
                .build();

        transactionRepository.save(transaction);
        accountRepository.save(account);

        return TransactionResponseDto.toWithdrawDto(transaction, fromPrevBalance, account.getBalance());
    }


    @Transactional
    public TransactionResponseDto send(UUID apiKey, TransactionRequestDto req) {

        Account fromAccount = accountRepository.findById(req.getFromAccount()).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));
        Account toAccount = accountRepository.findById(req.getToAccount()).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));

        Optional<Dummy> optionalDummy = Optional.ofNullable(req.getDummyId())
                .map(dummyId -> dummyRepository.findById(dummyId).orElseThrow(() -> new RestApiException(DummyErrorCode.NO_DUMMY)));

        bankAuthorityValidation(apiKey, fromAccount.getBankId());
        checkPassword(fromAccount, req.getPassword());

        if(fromAccount.getBalance() < req.getAmount())
            throw new RestApiException(TransactionErrorCode.NO_BALANCE);

        Long fromPrevBalance = fromAccount.getBalance();
        Long toPrevBalance = toAccount.getBalance();

        fromAccount.updateBalance(fromPrevBalance - req.getAmount());
        toAccount.updateBalance(toPrevBalance + req.getAmount());

        Transaction transaction = Transaction.builder()
                .amount(req.getAmount())
                .depositorName(req.getDepositorName() == null ? toAccount.getHolder().getName() : req.getDepositorName())
                .fromAccount(req.getFromAccount())
                .toAccount(req.getToAccount())
                .dummy(optionalDummy.orElse(null))
                .build();

        transactionRepository.save(transaction);
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        return TransactionResponseDto.toDto(transaction, fromPrevBalance, fromAccount.getBalance(), toPrevBalance, toAccount.getBalance());
    }

    @Transactional
    public TransactionUpdateResponseDto update(UUID apiKey, TransactionUpdateRequestDto req) {
        Transaction transaction = transactionRepository.findById(req.getTransactionId()).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));

        Long fromPrevBalance = 0L;
        Long fromBalance = 0L;
        Long toPrevBalance = 0L;
        Long toBalance = 0L;

        if(req.getDepositorName() != null)
            transaction.updateDepositorName(transaction.getDepositorName());
        transaction.updateAmount(req.getAmount());
        transaction.updateFromAccount(null);
        transaction.updateToAccount(transaction.getToAccount());

        if(req.getFromAccount() == null && req.getToAccount() != null){
            Account toAccount = accountRepository.findById(req.getToAccount()).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));
            toPrevBalance = toAccount.getBalance();
            toBalance = updateDeposit(apiKey, req);
        }

        else if(req.getFromAccount() != null && req.getToAccount() == null){
            Account fromAccount = accountRepository.findById(req.getFromAccount()).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));
            fromPrevBalance = fromAccount.getBalance();
            fromBalance = updateWithdraw(apiKey, req);
        }

        else if(req.getFromAccount() != null){
            Account fromAccount = accountRepository.findById(req.getFromAccount()).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));
            Account toAccount = accountRepository.findById(req.getToAccount()).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));
            toPrevBalance = toAccount.getBalance();
            fromPrevBalance = fromAccount.getBalance();
            Long[] balance = updateSend(apiKey, req);
            fromBalance = balance[0];
            toBalance = balance[1];
        }

        transactionRepository.save(transaction);

        return TransactionUpdateResponseDto.toDto(transaction, fromPrevBalance, fromBalance, toPrevBalance, toBalance);
    }

    public Long updateDeposit(UUID apiKey, TransactionUpdateRequestDto req) {
        Account account = accountRepository.findById(req.getToAccount()).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));

        bankAuthorityValidation(apiKey, account.getBankId());

        if (req.getAmount() != null) {
            //refund(req);
            account.updateBalance(account.getBalance() + req.getAmount());
        }

        accountRepository.save(account);

        return account.getBalance();
    }

    public Long updateWithdraw(UUID apiKey, TransactionUpdateRequestDto req) {
        Account account = accountRepository.findById(req.getFromAccount()).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));

        if(account.getBalance() < req.getAmount())
            throw new RestApiException(TransactionErrorCode.NO_BALANCE);

        bankAuthorityValidation(apiKey, account.getBankId());

        //refund(req);
        account.updateBalance(account.getBalance() - req.getAmount());

        accountRepository.save(account);

        return account.getBalance();
    }

    public Long[] updateSend(UUID apiKey, TransactionUpdateRequestDto req) {
        Account fromAccount = accountRepository.findById(req.getFromAccount()).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));
        Account toAccount = accountRepository.findById(req.getToAccount()).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));

        bankAuthorityValidation(apiKey, fromAccount.getBankId());

        if(fromAccount.getBalance() < req.getAmount())
            throw new RestApiException(TransactionErrorCode.NO_BALANCE);

        //refund(req);

        fromAccount.updateBalance(fromAccount.getBalance() - req.getAmount());
        toAccount.updateBalance(toAccount.getBalance() + req.getAmount());

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        return new Long[] {fromAccount.getBalance(), toAccount.getBalance()};
    }

    @Transactional
    public Transaction1wonResponseDto oneSend(UUID apiKey, Transaction1wonRequestDto req) {
        Account toAccount = accountRepository.findById(req.getAccountId()).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));

        String depositorName = Fourwords.chooseWord();

        Transaction transaction = Transaction.builder()
                .amount(1L)
                .depositorName(depositorName)
                .toAccount(req.getAccountId())
                .dummy(null)
                .build();

        toAccount.updateBalance(toAccount.getBalance() + 1);

        transactionRepository.save(transaction);
        accountRepository.save(toAccount);

        return Transaction1wonResponseDto.toDto(depositorName, transaction.getId());
    }

    public void oneSendConfirm(Transaction1wonConfirmRequestDto req) {
        Transaction transaction = transactionRepository.findById(req.getTransactionId()).orElseThrow(() -> new RestApiException(TransactionErrorCode.NO_TRANSACTION));

        if(!transaction.getDepositorName().equals(req.getWord()))
            throw new RestApiException(TransactionErrorCode.MiSMATCH);
    }

    @Transactional
    public void refund(TransactionUpdateRequestDto req) {
        Transaction transaction = transactionRepository.findById(req.getTransactionId()).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));
        Account fromAccount = accountRepository.findById(req.getFromAccount()).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));
        Account toAccount = accountRepository.findById(req.getToAccount()).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));

        Long preAmount = transaction.getAmount();

        if(toAccount.getBalance() < preAmount){
            throw new RestApiException(TransactionErrorCode.NO_REFUND);
        }

        fromAccount.updateBalance(fromAccount.getBalance() + preAmount);
        toAccount.updateBalance(toAccount.getBalance() - preAmount);

        if(req.getAmount() == null)
            throw new RestApiException(TransactionErrorCode.NO_AMOUNT);
        transaction.updateAmount(req.getAmount());
        transaction.updateDepositorName(req.getDepositorName());
        transaction.updateFromAccount(req.getFromAccount());
        transaction.updateFromAccount(req.getToAccount());

        transactionRepository.save(transaction);
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }

    @Transactional
    public void delete(TransactionDeleteRequestDto req) {
        Transaction transaction = transactionRepository.findById(req.getTransactionId()).orElseThrow(() -> new RestApiException(AccountErrorCode.NO_ACCOUNT));
        transaction.deleteSoftly();
    }

    @Transactional
    public void depositInterest(Account account, Long interest) {
        account.updateBalance(account.getBalance() + interest);

        Transaction transaction = Transaction.builder()
                .amount(interest)
                .depositorName("이자 지급")
                .fromAccount(null)
                .toAccount(account.getId())
                .build();

        transactionRepository.save(transaction);
        accountRepository.save(account);
    }

    @Transactional
    public void withdrawAmount(Account account) {
        if(account.getBalance() < account.getAmount())
            return;

        account.updateBalance(account.getBalance() - account.getAmount());

        Transaction transaction = Transaction.builder()
                .amount(account.getAmount())
                .depositorName("적금")
                .fromAccount(null)
                .toAccount(account.getId())
                .build();

        transactionRepository.save(transaction);
        accountRepository.save(account);
    }

    public void checkPassword(Account account, String password){
        if (!account.getPassword().equals(password))
            throw new RestApiException(AccountErrorCode.PASSWORD_MISMATCH);
    }

    public Page<TransactionSearchResponseDto> search(TransactionSearchRequestDto req, Pageable pageable) {
        return transactionRepository.searchTransactionCustom(req, pageable);
    }

    public void bankAuthorityValidation(UUID apiKey, UUID bankId) {
        UUID adminId = apiRepository.getByApiKey(apiKey).getAdminId();
        Bank bank = bankRepository.findById(bankId).orElseThrow(() -> new RestApiException(BankErrorCode.NO_BANK));
        if (!bank.getAdminId().equals(adminId))
            throw new RestApiException(CommonErrorCode.NO_AUTHORIZATION);
    }
}
