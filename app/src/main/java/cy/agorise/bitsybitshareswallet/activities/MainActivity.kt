package cy.agorise.bitsybitshareswallet.activities

import android.os.Bundle
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.models.JsonRpcResponse

class MainActivity : ConnectedActivity() {
    private val TAG = this.javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun handleJsonRpcResponse(response: JsonRpcResponse<*>) {

    }

    /**
     * Private method called whenever there's an update to the connection status
     * @param connectionStatusUpdate  Connection status update.
     */
    override fun handleConnectionStatusUpdate(connectionStatusUpdate: ConnectionStatusUpdate) {

    }
}
