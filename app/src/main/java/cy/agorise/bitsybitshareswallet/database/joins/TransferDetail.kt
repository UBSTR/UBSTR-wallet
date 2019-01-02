package cy.agorise.bitsybitshareswallet.database.joins

data class TransferDetail(
    val id: String,
    val from: String?,
    val to: String?,
    val direction: Boolean, // True -> Received, False -> Sent
    val memo: String,
    val date: Long,
    val cryptoAmount: Long,
    val cryptoPrecision: Int,
    val cryptoSymbol: String
//    val fiatAmount: Long,
//    val fiatCurrency: String
)