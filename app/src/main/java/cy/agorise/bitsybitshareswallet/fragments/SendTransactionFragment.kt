package cy.agorise.bitsybitshareswallet.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import cy.agorise.bitsybitshareswallet.R
import kotlinx.android.synthetic.main.fragment_send_transaction.*
import me.dm7.barcodescanner.zxing.ZXingScannerView

class SendTransactionFragment : Fragment(), ZXingScannerView.ResultHandler {

    // Camera Permission
    private val REQUEST_CAMERA_PERMISSION = 1

    private var isCameraPreviewVisible = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_send_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        verifyCameraPermission()

        fabOpenCamera.setOnClickListener { if (isCameraPreviewVisible) stopCameraPreview() else verifyCameraPermission() }
    }

    private fun verifyCameraPermission() {
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not already granted
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            // Permission is already granted
            startCameraPreview()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startCameraPreview()
            } else {
                // TODO extract string resource
                Toast.makeText(context!!, "Camera permission is necessary to read QR codes.", Toast.LENGTH_SHORT).show()
            }
            return
        }
    }

    private fun startCameraPreview() {
        cameraPreview.visibility = View.VISIBLE
        fabOpenCamera.setImageResource(R.drawable.ic_close)
        isCameraPreviewVisible = true

        // Configure QR scanner
        cameraPreview.setFormats(listOf(BarcodeFormat.QR_CODE))
        cameraPreview.setAspectTolerance(0.5f)
        cameraPreview.setAutoFocus(true)
        cameraPreview.setLaserColor(R.color.colorAccent)
        cameraPreview.setMaskColor(R.color.colorAccent)
        cameraPreview.setResultHandler(this)
        cameraPreview.startCamera()
    }

    private fun stopCameraPreview() {
        cameraPreview.visibility = View.INVISIBLE
        fabOpenCamera.setImageResource(R.drawable.ic_camera)
        isCameraPreviewVisible = false
        cameraPreview.stopCamera()
    }

    override fun handleResult(result: Result?) {
        Toast.makeText(context!!, result!!.text, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (isCameraPreviewVisible)
            startCameraPreview()
    }

    override fun onPause() {
        super.onPause()
        if (!isCameraPreviewVisible)
            stopCameraPreview()
    }
}
