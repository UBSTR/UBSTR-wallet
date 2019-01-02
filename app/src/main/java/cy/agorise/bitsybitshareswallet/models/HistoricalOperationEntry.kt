package cy.agorise.bitsybitshareswallet.models

import cy.agorise.graphenej.AssetAmount
import cy.agorise.graphenej.models.OperationHistory
import cy.agorise.graphenej.operations.TransferOperation

/**
 * This class is very similar to the OperationHistory, but while the later is used to deserialize
 * the transfer information exactly as it comes from the 'get_relative_account_history' API call,
 * this class is used to represent a single entry in the local database.
 *
 *
 * Every entry in the transfers table needs a bit more information than what is provided by the
 * HistoricalTransfer. We need to know the specific timestamp of a transaction for instance, instead
 * of just a block number.
 *
 *
 * There's also the data used for the equivalent fiat value.
 *
 *
 * Created by nelson on 12/18/16.
 */
class HistoricalOperationEntry {
    var historicalTransfer: OperationHistory? = null
    var timestamp: Long = 0
    var equivalentValue: AssetAmount? = null

    override fun toString(): String {
        if (historicalTransfer != null) {
            // Since for now we know that all stored historical operations are 'transfers'
            val op = historicalTransfer!!.operation as TransferOperation
            var memo = "?"
            if (op.memo != null && op.memo.plaintextMessage != null) {
                memo = op.memo.plaintextMessage
            }
            return String.format(
                "<%d, %s -> %s, %d of %s, memo: %s>",
                timestamp,
                op.from.objectId,
                op.to.objectId,
                op.assetAmount.amount.toLong(),
                op.assetAmount.asset.objectId,
                memo
            )
        } else {
            return "<>"
        }
    }

    override fun hashCode(): Int {
        return historicalTransfer!!.objectId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        return if (javaClass != other.javaClass) {
            false
        } else {
            val otherEntry = other as HistoricalOperationEntry?
            otherEntry!!.historicalTransfer!!.objectId == this.historicalTransfer!!.objectId
        }
    }
}
