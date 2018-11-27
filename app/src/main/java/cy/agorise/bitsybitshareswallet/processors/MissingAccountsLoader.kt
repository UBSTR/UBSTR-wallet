//package cy.agorise.bitsybitshareswallet.processors
//
//import android.content.Context
//import androidx.lifecycle.Lifecycle
//import cy.agorise.graphenej.api.android.RxBus
//import cy.agorise.graphenej.api.calls.GetAccounts
//import cy.agorise.graphenej.entities.AccountProperties
//import cy.agorise.graphenej.entities.JsonRpcResponse
//import io.reactivex.android.schedulers.AndroidSchedulers
//
///**
// * Loader used to fetch the missing accounts and update the database
// */
//class MissingAccountsLoader(context: Context, lifecycle: Lifecycle) : BaseDataLoader(context, lifecycle) {
//
//    init {
//        mDisposable = RxBus.getBusInstance()
//            .asFlowable()
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe { message ->
//                if (message is JsonRpcResponse<*>) {
//                    if (message.result is List<*> &&
//                        (message.result as List<*>).size > 0 &&
//                        (message.result as List<*>)[0] is AccountProperties
//                    ) {
//                        database.putUserAccounts(message.result as List<AccountProperties>)
//                    }
//                }
//            }
//    }
//
//    protected fun onNetworkReady() {
//        requestAccountInfo()
//    }
//
//    fun requestAccountInfo() {
//        val missingAccountNames = database.getMissingAccountNames()
//        if (missingAccountNames.size > 0) {
//            mNetworkService.sendMessage(GetAccounts(missingAccountNames), GetAccounts.REQUIRED_API)
//        }
//    }
//}
