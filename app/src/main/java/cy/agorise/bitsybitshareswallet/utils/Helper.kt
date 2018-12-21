package cy.agorise.bitsybitshareswallet.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Contains methods that are helpful in different parts of the app
 */
class Helper {

    companion object {
        private val TAG = "Helper"

        /**
         * Creates and returns a Bitmap from the contents of a View, does not matter
         * if it is a simple view or a ViewGroup like a ConstraintLayout or a LinearLayout.
         *
         * @param view The view that is gonna be pictured.
         * @return The generated image from the given view.
         */
        fun loadBitmapFromView(view: View): Bitmap {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)

            return bitmap
        }

        fun saveTemporalBitmap(context: Context, bitmap: Bitmap): Uri {
            // save bitmap to cache directory
            try {
                val cachePath = File(context.cacheDir, "images")
                if (!cachePath.mkdirs())
                // don't forget to make the directory
                    Log.d(TAG, "shareBitmapImage creating cache images folder")

                val stream = FileOutputStream(cachePath.toString() + "/image.png") // overwrites this image every time
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()
            } catch (e: IOException) {
                Log.d(TAG, "shareBitmapImage error: " + e.message)
            }

            // Send intent to share image+text
            val imagePath = File(context.cacheDir, "images")
            val newFile = File(imagePath, "image.png")

            // Create and return image uri
            return FileProvider.getUriForFile(context, "cy.agorise.FileProvider", newFile)
        }
    }
}