package cy.agorise.bitsybitshareswallet.utils

import android.content.Context
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import com.itextpdf.text.Document
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetail
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.lang.Exception
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

/**
 * AsyncTask subclass used to move the PDF generation procedure to a background thread
 * and inform the UI of the progress.
 */
class PDFGeneratorTask(context: Context) : AsyncTask<List<TransferDetail>, Int, String>() {

    companion object {
        private const val TAG = "PDFGeneratorTask"
    }

    private val mContext: WeakReference<Context> = WeakReference(context)

    override fun doInBackground(vararg params: List<TransferDetail>): String {
        return createPDFDocument(params[0])
    }

    private fun createPDFDocument(transferDetails: List<TransferDetail>): String {
        val document = Document(PageSize.A4.rotate())
        return try {
            // Create and configure a new PDF file to save the transfers list
            val externalStorageFolder = Environment.getExternalStorageDirectory().absolutePath + File.separator +
                    Constants.EXTERNAL_STORAGE_FOLDER
            val fileName = mContext.get()?.resources?.let {
                "${it.getString(R.string.app_name)}-${it.getString(R.string.title_transactions)}"} + ".pdf"
            val filePath = combinePath(externalStorageFolder, fileName)
            createEmptyFile(filePath)
            PdfWriter.getInstance(document, FileOutputStream(filePath))
            document.open()

            // Configure pdf table with 8 columns
            val table = PdfPTable(7)

            // Add the table header
            val columnNames = arrayOf(R.string.title_from, R.string.title_to, R.string.title_memo, R.string.title_date,
                                    R.string.title_time, R.string.title_asset_amount, R.string.title_fiat_equivalent)
            for (columnName in columnNames) {
                val cell = PdfPCell(Paragraph(mContext.get()?.getString(columnName)))
                table.addCell(cell)
            }
            table.completeRow()

            // Configure date and time formats to reuse in all the transfers
            val locale = mContext.get()?.resources?.configuration?.locale
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("MM-dd-yyyy", locale)
            val timeFormat = SimpleDateFormat("HH:MM:ss", locale)

            // Save all the transfers information
            for ( (index, transferDetail) in transferDetails.withIndex()) {
                calendar.timeInMillis = transferDetail.date * 1000
                val date = calendar.time

                table.addCell(makeCell(transferDetail.from ?: ""))          // From
                table.addCell(makeCell(transferDetail.to ?: ""))            // To
                table.addCell(makeCell(transferDetail.memo))                     // Memo
                table.addCell(makeCell(dateFormat.format(date)))                 // Date
                table.addCell(makeCell(timeFormat.format(date)))                 // Time

                // Asset Amount
                val assetPrecision = transferDetail.assetPrecision
                val assetAmount = transferDetail.assetAmount.toDouble() / Math.pow(10.0, assetPrecision.toDouble())
                table.addCell(makeCell(String.format("%.${assetPrecision}f %s", assetAmount, transferDetail.assetSymbol)))

                // Fiat Equivalent TODO add once Nelson finishes

                table.completeRow()

                // TODO update progress
            }
            document.add(table)
            document.close()
            "PDF generated and saved: $filePath"
        } catch (e: Exception) {
            Log.e(TAG, "Exception while trying to generate a PDF. Msg: " + e.message)
            "Unable to generate PDF. Please retry. Error: ${e.message}"
        }
    }

    private fun combinePath(path1: String, path2: String): String {
        val file1 = File(path1)
        val file2 = File(file1, path2)
        return file2.path
    }

    /** Creates an empty file with the given name, in case it does not exist */
    private fun createEmptyFile(path: String) {
        try {
            val file = File(path)
            val writer = FileWriter(file)
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    /** Hides the simple but repetitive logic of creating a PDF table cell */
    private fun makeCell(text: String) : PdfPCell {
        return PdfPCell(Paragraph(text))
    }

    override fun onProgressUpdate(values: Array<Int>) {
        // TODO show progress
    }

    override fun onPostExecute(message: String) {
        mContext.get()?.toast(message)
    }
}