package com.personal.finance.transaction.controller;

import com.personal.finance.transaction.dto.response.ReportsSummaryResponse;
import com.personal.finance.transaction.service.ReportsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Dashboard / Reports aggregate endpoint — REQ-reports-summary.md.
 *
 * <p>One read replaces what would otherwise be ≥4 chatty (and size-page
 * limited) {@code /transactions} fetches plus N {@code /balances?asOf=…}
 * calls for the net-worth trend.
 */
@RestController
@RequestMapping("/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "One-shot aggregates for the Dashboard and Reports screens.")
public class ReportsController {

    private final ReportsService reportsService;

    @Operation(
            summary = "Dashboard / Reports summary",
            description = "Aggregates net worth, spend by category, 12-month income / spend trend, and top merchants "
                    + "in a single response. Designed for the Dashboard (consumes everything except topMerchants) "
                    + "and the Reports page (consumes spendByCategory, topMerchants, and both trend arrays). "
                    + "When transactions in scope span multiple currencies the top-level currency is null and the "
                    + "netWorth fields are null — naive cross-currency sums would mislead the user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aggregated summary",
                    content = @Content(schema = @Schema(implementation = ReportsSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure — bad range or future asOf",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated — missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.personal.finance.common.web.ApiResponse.class)))
    })
    @GetMapping("/summary")
    public ReportsSummaryResponse summary(
            @Parameter(in = ParameterIn.HEADER, name = TransactionController.USER_ID_HEADER,
                    description = "Authenticated user id injected by the gateway", required = true,
                    schema = @Schema(type = "string", example = "user_abc123"))
            @RequestHeader(TransactionController.USER_ID_HEADER) String userId,
            @Parameter(description = "Window for spendByCategory / topMerchants and netWorth comparison reference. "
                    + "month = MTD; year = YTD. Trend arrays are always 12 monthly buckets.",
                    schema = @Schema(type = "string", allowableValues = {"month", "year"}, example = "month"),
                    required = true)
            @RequestParam("range") String range,
            @Parameter(description = "Cut-off date (inclusive). Defaults to today. Must not be in the future.",
                    schema = @Schema(type = "string", format = "date", example = "2026-06-04"))
            @RequestParam(value = "asOf", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
            @Parameter(description = "Restrict every aggregate to these account UUIDs. Omit (or pass empty) "
                    + "to span every account. Empty list short-circuits to all-zero / empty response. "
                    + "v1 does not validate ownership — unowned ids contribute zero (spec §2.1).")
            @RequestParam(value = "accountIds", required = false) List<UUID> accountIds) {
        return reportsService.getSummary(userId, range, asOf, accountIds);
    }
}
