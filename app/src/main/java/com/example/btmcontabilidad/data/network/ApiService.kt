package com.example.btmcontabilidad.data.network

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.PUT

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiEnvelope<LoginData>>

    @GET("auth/me")
    suspend fun me(@Header("Authorization") bearerToken: String): Response<ApiEnvelope<ApiUser>>

    @GET("dashboard")
    suspend fun dashboard(@Header("Authorization") bearerToken: String): Response<ApiEnvelope<DashboardResponse>>

    @GET("collections")
    suspend fun collections(@Header("Authorization") bearerToken: String): Response<ApiEnvelope<List<CollectionResponse>>>

    @GET("advances")
    suspend fun advances(@Header("Authorization") bearerToken: String): Response<ApiEnvelope<List<AdvanceResponse>>>

    @GET("branches")
    suspend fun branches(@Header("Authorization") bearerToken: String): Response<ApiEnvelope<List<BranchResponse>>>

    @GET("branches/{branchId}")
    suspend fun branch(
        @Header("Authorization") bearerToken: String,
        @Path("branchId") branchId: String
    ): Response<ApiEnvelope<BranchResponse>>

    @POST("branches")
    suspend fun createBranch(
        @Header("Authorization") bearerToken: String,
        @Body request: BranchRequest
    ): Response<ApiEnvelope<BranchResponse>>

    @PUT("branches/{branchId}")
    suspend fun updateBranch(
        @Header("Authorization") bearerToken: String,
        @Path("branchId") branchId: String,
        @Body request: BranchRequest
    ): Response<ApiEnvelope<BranchResponse>>

    @DELETE("branches/{branchId}")
    suspend fun deleteBranch(
        @Header("Authorization") bearerToken: String,
        @Path("branchId") branchId: String
    ): Response<ApiEnvelope<Any>>

    @GET("ledger")
    suspend fun ledger(
        @Header("Authorization") bearerToken: String,
        @Query("branch_id") branchId: String? = null
    ): Response<ApiEnvelope<List<LedgerEntryResponse>>>

    @POST("collections")
    suspend fun createCollection(
        @Header("Authorization") bearerToken: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateCollectionRequest
    ): Response<ApiEnvelope<CollectionResponse>>

    @POST("advances")
    suspend fun createAdvance(
        @Header("Authorization") bearerToken: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateAdvanceRequest
    ): Response<ApiEnvelope<AdvanceResponse>>

    @GET("weekly-settlements")
    suspend fun weeklySettlements(
        @Header("Authorization") bearerToken: String,
        @Query("branch_id") branchId: String? = null
    ): Response<ApiEnvelope<List<WeeklySettlementResponse>>>

    @POST("weekly-settlements")
    suspend fun createWeeklySettlement(
        @Header("Authorization") bearerToken: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateWeeklySettlementRequest
    ): Response<ApiEnvelope<WeeklySettlementResponse>>

    @POST("ledger-entries/{entryId}/reverse")
    suspend fun createReversal(
        @Header("Authorization") bearerToken: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Path("entryId") entryId: Long,
        @Body request: CreateReversalRequest
    ): Response<ApiEnvelope<LedgerEntryResponse>>

    @GET("cash-box")
    suspend fun cashBox(
        @Header("Authorization") bearerToken: String
    ): Response<ApiEnvelope<CashBoxResponse>>

    @POST("cash-box/income")
    suspend fun createCashIncome(
        @Header("Authorization") bearerToken: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CashMovementRequest
    ): Response<ApiEnvelope<CashMovementResponse>>

    @POST("cash-box/expenses")
    suspend fun createCashExpense(
        @Header("Authorization") bearerToken: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CashMovementRequest
    ): Response<ApiEnvelope<CashMovementResponse>>

    @POST("cash-box/branch-transfers")
    suspend fun createCashBranchTransfer(
        @Header("Authorization") bearerToken: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CashMovementRequest
    ): Response<ApiEnvelope<CashMovementResponse>>

    @GET("results")
    suspend fun results(
        @Header("Authorization") bearerToken: String,
        @Query("branch_id") branchId: Long? = null,
        @Query("business_date") businessDate: String? = null
    ): Response<ApiEnvelope<List<ManualResultResponse>>>

    @POST("results")
    suspend fun createResult(
        @Header("Authorization") bearerToken: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateManualResultRequest
    ): Response<ApiEnvelope<ManualResultResponse>>

    @GET("money-deliveries")
    suspend fun moneyDeliveries(
        @Header("Authorization") bearerToken: String,
        @Query("period") period: String? = null,
        @Query("branch_id") branchId: Long? = null
    ): Response<ApiEnvelope<MoneyDeliveryPayload>>

    @POST("money-deliveries")
    suspend fun createMoneyDelivery(
        @Header("Authorization") bearerToken: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateMoneyDeliveryRequest
    ): Response<ApiEnvelope<MoneyDeliveryResponse>>

    @GET("collectors")
    suspend fun collectors(
        @Header("Authorization") bearerToken: String
    ): Response<ApiEnvelope<List<CollectorDto>>>

    @POST("collectors")
    suspend fun createCollector(
        @Header("Authorization") bearerToken: String,
        @Body request: CreateCollectorRequest
    ): Response<ApiEnvelope<CollectorDto>>

    @PUT("collectors/{id}")
    suspend fun updateCollector(
        @Header("Authorization") bearerToken: String,
        @Path("id") id: Long,
        @Body request: UpdateCollectorRequest
    ): Response<ApiEnvelope<CollectorDto>>
}

@JsonClass(generateAdapter = true)
data class ApiEnvelope<T>(val success: Boolean = false, val message: String? = null, val data: T? = null)

@JsonClass(generateAdapter = true)
data class LoginRequest(val email: String, val password: String, val device_name: String)

@JsonClass(generateAdapter = true)
data class LoginData(val token: String, val user: ApiUser)

@JsonClass(generateAdapter = true)
data class ApiUser(val id: Long, val name: String, val email: String)

@JsonClass(generateAdapter = true)
data class CollectorDto(
    val id: Long,
    val name: String,
    val email: String,
    val role: String,
    val status: String,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateCollectorRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String = "collector",
    val status: String = "active"
)

@JsonClass(generateAdapter = true)
data class UpdateCollectorRequest(
    val name: String? = null,
    val email: String? = null,
    val password: String? = null,
    val role: String? = null,
    val status: String? = null
)


@JsonClass(generateAdapter = true)
data class CreateCollectionRequest(
    val branch_id: Long,
    val amount: String,
    val business_date: String,
    val payment_method: String,
    val reference: String? = null,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class CollectionResponse(
    val id: Long?,
    val branch_id: Long?,
    val amount: String?,
    val payment_method: String? = null,
    val reference: String? = null,
    val business_date: String? = null,
    val notes: String? = null,
    val status: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateAdvanceRequest(
    val branch_id: Long,
    val amount: String,
    val reason: String,
    val business_date: String,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateWeeklySettlementRequest(
    val branch_id: Long,
    val week_start: String,
    val week_end: String,
    val sales_amount: String,
    val prizes_amount: String,
    val commission_rate: String,
    val cash_delivered_amount: String,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class WeeklySettlementResponse(
    val id: Long?,
    val branch_id: Long?,
    val week_start: String?,
    val week_end: String?,
    val sales_amount: String?,
    val prizes_amount: String?,
    val commission_rate: String?,
    val commission_amount: String?,
    val cash_delivered_amount: String?,
    val weekly_balance: String?,
    val balance_before: String?,
    val balance_after: String?,
    val notes: String? = null,
    val status: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateReversalRequest(val business_date: String, val reason: String)

@JsonClass(generateAdapter = true)
data class CashMovementRequest(
    val amount: String,
    val business_date: String,
    val reason: String,
    val branch_id: Long? = null,
    val reference: String? = null,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class CashBoxResponse(
    val currency_code: String?,
    val current_balance: String?,
    val entries: List<CashMovementResponse>?
)

@JsonClass(generateAdapter = true)
data class CashMovementResponse(
    val id: Long?,
    val type: String? = null,
    val movement_type: String? = null,
    val amount: String?,
    val business_date: String?,
    val reason: String? = null,
    val branch_id: Long? = null,
    val reference: String? = null,
    val notes: String? = null,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class BranchResponse(
    val id: Long?,
    val code: String?,
    val name: String?,
    val phone: String? = null,
    val owner_name: String? = null,
    val owner_phone: String? = null,
    val description: String? = null,
    val route: String? = null,
    val operator_name: String? = null,
    val current_balance: String? = null,
    val status: String? = null,
    val collections: List<CollectionResponse>? = null,
    val advances: List<AdvanceResponse>? = null,
    val weekly_settlements: List<WeeklySettlementResponse>? = null,
    val ledger: List<LedgerEntryResponse>? = null
)

@JsonClass(generateAdapter = true)
data class BranchRequest(
    val code: String,
    val name: String,
    val phone: String? = null,
    val owner_name: String? = null,
    val owner_phone: String? = null,
    val description: String? = null,
    val route: String? = null,
    val operator_name: String? = null,
    val status: String = "active"
)


@JsonClass(generateAdapter = true)
data class AdvanceResponse(
    val id: Long?,
    val branch_id: Long?,
    val amount: String?,
    val reason: String? = null,
    val business_date: String? = null,
    val notes: String? = null,
    val status: String? = null
)

@JsonClass(generateAdapter = true)
data class LedgerEntryResponse(
    val id: Long?,
    val branch_id: Long?,
    val source_type: String? = null,
    val source_id: Long? = null,
    val entry_type: String? = null,
    val signed_amount: String? = null,
    val balance_before: String? = null,
    val balance_after: String? = null,
    val business_date: String? = null,
    val description: String? = null,
    val created_by: String? = null,
    val reversal_of_entry_id: Long? = null
)

@JsonClass(generateAdapter = true)
data class DashboardResponse(
    val receivable_total: String? = null,
    val branch_credit_total: String? = null,
    val net_position: String? = null,
    val collections_total: String? = null,
    val advances_total: String? = null,
    val currency_code: String? = null,
    val active_branches_count: Int? = null,
    val positive_branches_count: Int? = null,
    val negative_branches_count: Int? = null,
    val zero_branches_count: Int? = null,
    val total_pending_to_collect: String? = null,
    val total_to_collect_next_monday: String? = null,
    val total_money_delivered_this_week: String? = null,
    val total_money_delivered_this_month: String? = null,
    val total_collected_this_week: String? = null,
    val total_collected_this_month: String? = null,
    val recent_activity: List<LedgerEntryResponse>? = null
)

@JsonClass(generateAdapter = true)
data class CreateManualResultRequest(
    val branch_id: Long,
    val amount: String,
    val business_date: String,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class ManualResultResponse(
    val id: Long?,
    val branch_id: Long?,
    val amount: String?,
    val classification: String?,
    val business_date: String?,
    val notes: String? = null,
    val status: String? = null,
    val requires_money_delivery: Boolean? = null,
    val balance_after: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateMoneyDeliveryRequest(
    val branch_id: Long,
    val amount: String,
    val suggested_amount: String? = null,
    val manual_result_id: Long? = null,
    val business_date: String,
    val reason: String,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class MoneyDeliveryResponse(
    val id: Long?,
    val branch_id: Long?,
    val manual_result_id: Long? = null,
    val suggested_amount: String? = null,
    val delivered_amount: String?,
    val business_date: String?,
    val reason: String?,
    val notes: String? = null,
    val status: String? = null,
    val branch_balance_after: String? = null
)

@JsonClass(generateAdapter = true)
data class MoneyDeliveryPayload(
    val deliveries: List<MoneyDeliveryResponse>? = null,
    val total_delivered: String? = null,
    val by_branch: List<MoneyDeliveryByBranchResponse>? = null
)

@JsonClass(generateAdapter = true)
data class MoneyDeliveryByBranchResponse(
    val branch_id: Long?,
    val branch_name: String?,
    val total: String?,
    val count: Int? = null
)
