package cy.agorise.bitsybitshareswallet.database.joins

data class TransferDetail(
    val id: String,
    val from: String?,
    val to: String?,
    val direction: Boolean, // True -> Received, False -> Sent
    val memo: String,
    val date: Long,
    val assetAmount: Long,
    val assetPrecision: Int,
    val assetSymbol: String,
    val assetIssuer: String,
    val fiatAmount: Long?,
    val fiatSymbol: String?
) {
    fun getUIAssetSymbol(): String {
        if (assetIssuer == "1.2.0")
            return "bit$assetSymbol"
        return assetSymbol
    }
}