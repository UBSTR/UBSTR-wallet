package cy.agorise.bitsybitshareswallet;

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BitsyDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        helper.createDatabase(TEST_DB, 2).apply {
            // db has schema version 1. insert some data using SQL queries.
            // You cannot use DAO classes because they expect the latest schema.
            execSQL("INSERT INTO assets(id, symbol, precision, description, bit_asset_id) VALUES('1.3.0','BTS', 5, '', '')")
            // Inserting two entries in the 'transfers' table, without any 'bts_value' field.
            execSQL("INSERT INTO transfers(id, block_number, timestamp, fee_amount, fee_asset_id, source, destination, transfer_amount, transfer_asset_id, memo) values('1',0,1500000000,120,'1.3.0','1.2.100','1.2.101',1000,'1.3.121','')")
            execSQL("INSERT INTO transfers(id, block_number, timestamp, fee_amount, fee_asset_id, source, destination, transfer_amount, transfer_asset_id, memo) values('2',1,1500000300,120,'1.3.0','1.2.100','1.2.101',1000,'1.3.0','')")
            execSQL("INSERT INTO equivalent_values(id, transfer_id, value, asset_id) values(1, 1, 100, '1.3.0')")
            // Prepare for the next version.
            close()
        }
        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, BitsyDatabase.MIGRATION_2_3)
        val cursor = db.query("SELECT bts_value FROM transfers WHERE id = '2'")
        cursor.moveToFirst()
        // Checking that the 'bts_value' of the transfer with id '2', which was a BTS transfer, now has the 'bts_value'
        // column with the same value as the 'transfer_amount'.
        Assert.assertEquals(1, cursor.count)
        Assert.assertEquals(1, cursor.columnCount)
        Assert.assertEquals(1000, cursor.getLong(0))
    }
}
