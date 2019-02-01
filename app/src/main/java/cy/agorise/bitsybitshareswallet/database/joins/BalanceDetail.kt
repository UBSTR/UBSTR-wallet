package cy.agorise.bitsybitshareswallet.database.joins

data class BalanceDetail(
    val id: String,
    val amount: Long,
    val precision: Int,
    val symbol: String,
    val issuer: String
) {
    // Add the bit prefix to smartcoins, ie bitUSD, bitEUR, bitMXN, etc.
    override fun toString(): String {
        if (issuer == "1.2.0")
            return "bit$symbol"
        return symbol
    }
}