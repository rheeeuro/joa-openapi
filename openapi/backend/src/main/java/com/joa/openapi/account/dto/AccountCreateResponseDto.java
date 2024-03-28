package com.joa.openapi.account.dto;

import com.joa.openapi.account.entity.Account;
import lombok.*;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreateResponseDto {

    private String accountId;
    private String productName;
    private String nickname;
    private Long balance;
    private Boolean isDormant;
    private Long transferLimit;
    private String startDate;
    private String endDate;
    private Integer term;
    private String depositAccount;
    private String withdrawAccount;
    private Long amount;
    private UUID dummyId;
    private UUID bankId;
    private UUID productId;
    private Double calculatedInterest;

    public static AccountCreateResponseDto toDto(Account account, Double calculatedInterest) {
        return AccountCreateResponseDto.builder()
                .accountId(account.getId())
                .productName(account.getProduct().getName())
                .nickname(account.getName())
                .balance(account.getBalance())
                .isDormant(account.getIsDormant())
                .transferLimit(account.getTransferLimit())
                .startDate(account.getStartDate())
                .endDate(account.getEndDate())
                .term(account.getTerm())
                .depositAccount(account.getDepositAccount())
                .withdrawAccount(account.getWithdrawAccount())
                .amount(account.getAmount())
                .dummyId(account.getDummy() == null ? null : account.getDummy().getId())
                .bankId(account.getBankId())
                .productId(account.getProduct().getId())
                .calculatedInterest(calculatedInterest)
                .build();
    }
}
