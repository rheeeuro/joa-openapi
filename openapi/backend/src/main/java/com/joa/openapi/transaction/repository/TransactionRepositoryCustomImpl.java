package com.joa.openapi.transaction.repository;

import static com.joa.openapi.account.entity.QAccount.account;
import static com.joa.openapi.bank.entity.QBank.bank;
import static com.joa.openapi.transaction.entity.QTransaction.transaction;

import com.joa.openapi.common.exception.RestApiException;
import com.joa.openapi.common.repository.ApiRepository;
import com.joa.openapi.transaction.dto.req.TransactionSearchRequestDto;
import com.joa.openapi.transaction.dto.res.DayMoneyFlow;
import com.joa.openapi.transaction.dto.res.TransactionSearchResponseDto;
import com.joa.openapi.transaction.entity.Transaction;
import com.joa.openapi.transaction.enums.TransactionOrderBy;
import com.joa.openapi.transaction.errorcode.TransactionErrorCode;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TransactionRepositoryCustomImpl implements TransactionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;
    private final JPAQueryFactory jpaQueryFactory; // JPA 쿼리를 생성하고 실행하는데 사용
    private final ApiRepository apiRepository;

    @Override
    public Page<TransactionSearchResponseDto> searchTransactionCustom(
        TransactionSearchRequestDto req, Pageable pageable) {

        BooleanBuilder condition = new BooleanBuilder();

        // API Key 확인 & bankId 조건 처리
        UUID apiKey = req.getApiKey();
        List<UUID> adminBankIds = getBankIdsByApiKey(apiKey);
        UUID bankId = req.getBankId();

        if (bankId == null) {
            // bankId선택 안했는데, admin의 은행들이 여러개인 경우
            if (!adminBankIds.isEmpty()) {
                List<String> accountIds = jpaQueryFactory.select(account.id)
                    .from(account)
                    .where(account.bankId.in(adminBankIds))
                    .orderBy(account.createdAt.desc())
                    .fetch();

                // 은행의 계좌가 하나라도 있을 때
                if (!accountIds.isEmpty()) {
                    condition.and(transaction.fromAccount.in(accountIds)
                        .or(transaction.toAccount.in(accountIds)));
                }

            } else {
                // adminBankIds가 비어있는 경우, 즉 admin에 할당된 은행이 없을 때
                return new PageImpl<>(Collections.emptyList(), pageable, 0); // 비어있는 결과 반환
            }

        } else {
            if (adminBankIds.contains(bankId)) {
                List<String> accountIds = jpaQueryFactory.select(account.id)
                    .from(account)
                    .where(account.bankId.eq(bankId))
                    .orderBy(account.createdAt.desc())
                    .fetch();

                if (!accountIds.isEmpty()) {
                    condition.and(transaction.fromAccount.in(accountIds)
                        .or(transaction.toAccount.in(accountIds)));
                }
            }
        }

        // isDummy 조건 처리
        Boolean isDummy = req.isDummy();
        if (Boolean.TRUE.equals(isDummy)) {
            condition.and(transaction.dummy.isNotNull());
        }

        // depositorName 조건 처리
        BooleanExpression depositorNameKeywordCondition = eqSearchDepositorNameKeyword(
            req.getDepositorNameKeyword());
        if (depositorNameKeywordCondition != null) {
            condition.and(depositorNameKeywordCondition);
        }

        // accountId 조건 처리
        String accountId = req.getAccountId();
        if (accountId != null) {
            condition.and(transaction.fromAccount.eq(accountId)
                .or(transaction.toAccount.eq(accountId)));
        }

        // dummyName 조건 처리
        String dummyName = req.getDummyName();
        if (dummyName != null) {
            condition.and(transaction.dummy.name.eq(dummyName));
        }

        // fromAmount, toAmount 조건 처리
        Long fromAmount = req.getFromAmount();
        Long toAmount = req.getToAmount();
        if (fromAmount != null && toAmount != null) {
            condition.and(transaction.amount.between(fromAmount, toAmount));
        }

        // 날짜 조건 처리 fromDate, toDate
        LocalDateTime fromDate = Optional.ofNullable(req.getFromDate())
            .map(date -> date.atTime(0, 0))
            .orElse(LocalDateTime.of(1900, 1, 1, 0, 0)); // 최소 날짜 기본값
        LocalDateTime toDate = Optional.ofNullable(req.getToDate())
            .map(date -> date.atTime(23, 59, 59))
            .orElse(LocalDateTime.of(3000, 12, 31, 23, 59, 59)); // 최대 날짜 기본값

        condition.and(transaction.createdAt.between(fromDate, toDate));

        // 검색 조건 적용 searchType
        BooleanExpression searchTypeCondition = eqSearchType(req);
        if (searchTypeCondition != null) {
            condition.and(searchTypeCondition);
        }

        // 쿼리 설정
        JPAQuery<Transaction> query = jpaQueryFactory
            .selectFrom(transaction)
            .where(condition)
            .orderBy(transaction.createdAt.desc());

        // 정렬 조건 적용 orderBy
        OrderSpecifier<?> orderSpecifier = eqOrderBy(req.getOrderBy());
        if (orderSpecifier != null) {
            query = query.orderBy(orderSpecifier);
        }

        long total = query.fetchCount();

        // 페이지네이션 적용
        List<Transaction> transactions = query
            .offset(pageable.getOffset())   // 반환되는 행의 시작점
            .limit(pageable.getPageSize())  // 반환되는 행의 수
            .fetch();

        // DTO 변환
        List<TransactionSearchResponseDto> res = transactions.stream()
            .map(TransactionSearchResponseDto::toDto)
            .collect(Collectors.toList());

        return new PageImpl<>(res, pageable, total);
    }

    @Override
    public Long searchBanksTotalTransactionCustom(UUID bankId) {
        return jpaQueryFactory
                .selectFrom(transaction)
                .distinct()
                .innerJoin(account).on(transaction.toAccount.eq(account.id).or(transaction.fromAccount.eq(account.id)))
                .where(account.bankId.eq(bankId))
                .fetchCount();
    }

    @Override
    public Long searchBanksTotalWithdrawCustom(UUID bankId) {
        Long query = jpaQueryFactory
                .select(transaction.amount.sum())
                .from(transaction)
                .distinct()
                .innerJoin(account).on(transaction.fromAccount.eq(account.id))
                .where(account.bankId.eq(bankId))
                .fetchOne();
        return query == null ? 0 : query;
    }

    @Override
    public Long searchBanksTotalDepositCustom(UUID bankId) {
        Long query = jpaQueryFactory
                .select(transaction.amount.sum())
                .from(transaction)
                .distinct()
                .innerJoin(account).on(transaction.toAccount.eq(account.id))
                .where(account.bankId.eq(bankId))
                .fetchOne();
        return query == null ? 0 : query;
    }

    @Override
    public List<DayMoneyFlow> searchBanksWeekTransactionCustom(UUID bankId) {
        List<String> accountIdList = jpaQueryFactory
                .select(account.id)
                .from(account)
                .where(account.bankId.eq(bankId))
                .fetch();

        return jpaQueryFactory
                .select(
                        Expressions.dateTemplate(LocalDate.class, "DATE_FORMAT({0}, '%Y-%m-%d')", transaction.createdAt),
                        new CaseBuilder()
                                .when(transaction.toAccount.isNotNull())
                                .then(transaction.amount)
                                .otherwise(0L).sum()
                                .as("deposit"),
                        new CaseBuilder()
                                .when(transaction.fromAccount.isNotNull())
                                .then(transaction.amount)
                                .otherwise(0L).sum()
                                .as("withdraw")
                )
                .from(transaction)
                .where(transaction.createdAt.between(LocalDateTime.now().minusWeeks(1), LocalDateTime.now())
                        .and(transaction.toAccount.in(accountIdList)
                                .or(transaction.fromAccount.in(accountIdList))))
                .groupBy(Expressions.dateTemplate(LocalDate.class, "DATE_FORMAT({0}, {1})", transaction.createdAt, "%Y-%m-%d"))
                .orderBy(Expressions.dateTemplate(LocalDate.class, "DATE_FORMAT({0}, {1})", transaction.createdAt, "%Y-%m-%d").asc())
                .fetch()
                .stream()
                .map(tuple -> new DayMoneyFlow(
                        tuple.get(0, String.class),
                        tuple.get(1, Long.class),
                        tuple.get(2, Long.class)
                ))
                .collect(Collectors.toList());
    }

    private BooleanExpression eqSearchDepositorNameKeyword(String depositorNameKeyword) {
        if (depositorNameKeyword == null || depositorNameKeyword.isBlank()) {
            return null;
        }

        return transaction.depositorName.likeIgnoreCase("%" + depositorNameKeyword + "%");

    }

    private BooleanExpression eqSearchType(TransactionSearchRequestDto req) {
        if (req.getSearchType() == null) {
            return null;
        }
        if (req.getAccountId() == null || req.getAccountId().isBlank()) {
            return null;
        }
        return switch (req.getSearchType()) {
            case DEPOSIT_ONLY -> transaction.toAccount.eq(req.getAccountId());
            case WITHDRAWAL_ONLY -> transaction.fromAccount.eq(req.getAccountId());
            default -> transaction.fromAccount.eq(req.getAccountId())
                .or(transaction.toAccount.eq(req.getAccountId()));
        };
    }

    private OrderSpecifier<?> eqOrderBy(TransactionOrderBy orderBy) {
        if (orderBy == null) {
            return null;
        }
        return switch (orderBy) {
            case OLDEST -> transaction.createdAt.asc();
            case AMOUNT_ASC -> transaction.amount.asc();
            case AMOUNT_DESC -> transaction.amount.desc();
            default -> transaction.createdAt.desc();
        };
    }

    private List<UUID> getBankIdsByApiKey(UUID apiKey) {
        UUID adminId = apiRepository.getByApiKey(apiKey).getAdminId();

        // 관리자 ID를 통해 해당 관리자가 만든 bankId 목록 가져오기
        List<UUID> bankIds = jpaQueryFactory.select(bank.id)
            .from(bank)
            .where(bank.adminId.eq(adminId))
            .fetch();

        return bankIds;
    }

}
