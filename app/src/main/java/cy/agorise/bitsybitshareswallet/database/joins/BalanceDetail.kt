package cy.agorise.bitsybitshareswallet.database.joins

data class BalanceDetail(
    val id: String,
    val amount: Long,
    val precision: Int,
    val symbol: String
)