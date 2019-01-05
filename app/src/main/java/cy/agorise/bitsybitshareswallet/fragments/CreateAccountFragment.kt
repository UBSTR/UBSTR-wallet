package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.models.JsonRpcResponse

class CreateAccountFragment : ConnectedFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.fragment_create_account, container, false)
    }

    override fun handleJsonRpcResponse(response: JsonRpcResponse<*>) {
        // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun handleConnectionStatusUpdate(connectionStatusUpdate: ConnectionStatusUpdate) {
        // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}