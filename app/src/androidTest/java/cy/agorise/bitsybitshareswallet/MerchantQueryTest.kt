package cy.agorise.bitsybitshareswallet;

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.AndroidJUnit4
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.database.entities.Merchant
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
public class MerchantQueryTest {
    private lateinit var db: BitsyDatabase

    @Before
    fun createDb(){
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BitsyDatabase::class.java).build()

        // Creating a few sample merchants
        val merchant1 = Merchant("5c23c8c234c83f0013e67786",
            "Café del Mar",
            "Address1",
            17.82834,
            -9.38483,
            "+1 999.999.9999",
            "@user",
            "https://100Natural.com")
        val merchant2 = Merchant("5c23c91a34c83f0013e67787",
            "Condesa Acapulco",
            "Address2",
            13.82834,
            -67.38483,
            "+1 999.999.9999",
            "@user",
            "https://100Natural.com")
        db.merchantDao().insert(merchant1)
        db.merchantDao().insert(merchant2)
    }

    @After
    @Throws(IOException::class)
    fun closeDb(){
        db.close()
    }

    @Test
    fun testSimpleQuery(){
        val allMerchants = db.merchantDao().getAllSync()
        Assert.assertEquals(2, allMerchants.size)
    }

    @Test
    fun testSearchQuery(){
        val query = "%con%"
        val merchants = db.merchantDao().findMerchantsByWordSync(query)
        Assert.assertEquals(1, merchants.size)
        Assert.assertEquals(merchants[0].name, "Condesa Acapulco")
    }
}
