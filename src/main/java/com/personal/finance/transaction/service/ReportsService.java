package com.personal.finance.transaction.service;

import com.personal.finance.transaction.dto.response.ReportsSummaryResponse;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

/**
 * {@code GET /v1/reports/summary} — Dashboard / Reports one-shot
 * aggregate. See REQ-reports-summary.md.
 */
public interface ReportsService {

    /**
     * @param userId     authenticated user (header-injected)
     * @param range      "month" or "year" — drives the {@code spendByCategory} /
     *                   {@code topMerchants} window and the {@code netWorth.previous}
     *                   rewind step. Invalid values throw {@code ValidationException}.
     * @param asOf       cut-off date (inclusive). Defaults to today when null;
     *                   future dates throw {@code ValidationException}.
     * @param accountIds optional scope filter — null or empty means "every txn
     *                   for this user". An explicitly empty list short-circuits
     *                   to zero / empty aggregates without touching the DB. v1
     *                   does NOT validate ownership against account-service
     *                   (spec §2.1).
     */
    ReportsSummaryResponse getSummary(String userId,
                                      String range,
                                      LocalDate asOf,
                                      Collection<UUID> accountIds);
}
