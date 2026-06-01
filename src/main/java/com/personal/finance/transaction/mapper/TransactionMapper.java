package com.personal.finance.transaction.mapper;

import com.personal.finance.transaction.dto.response.CategoryRefResponse;
import com.personal.finance.transaction.dto.response.TransactionResponse;
import com.personal.finance.transaction.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionMapper {

    /**
     * Mapping a transaction without category context — caller stitches the
     * category in via {@link #toResponse(Transaction, CategoryRefResponse)}.
     */
    @Mapping(target = "category", ignore = true)
    TransactionResponse toResponse(Transaction transaction);

    default TransactionResponse toResponse(Transaction transaction, CategoryRefResponse category) {
        if (transaction == null) return null;
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .accountId(transaction.getAccountId())
                .entryType(transaction.getEntryType())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .transactionDate(transaction.getTransactionDate())
                .reference(transaction.getReference())
                .description(transaction.getDescription())
                .source(transaction.getSource())
                .category(category)
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
