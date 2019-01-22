package cy.agorise.bitsybitshareswallet.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cy.agorise.bitsybitshareswallet.database.daos.*
import cy.agorise.bitsybitshareswallet.database.entities.*
import cy.agorise.bitsybitshareswallet.database.joins.BalanceDetailDao
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetailDao

@Database(entities = [
        Asset::class,
        Authority::class,
        Balance::class,
        EquivalentValue::class,
        Transfer::class,
        UserAccount::class,
        Merchant::class,
        Teller::class
    ],
    version = 2,
    exportSchema = true)
abstract class BitsyDatabase : RoomDatabase() {

    abstract fun assetDao(): AssetDao
    abstract fun authorityDao(): AuthorityDao
    abstract fun balanceDao(): BalanceDao
    abstract fun equivalentValueDao(): EquivalentValueDao
    abstract fun transferDao(): TransferDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun balanceDetailDao(): BalanceDetailDao
    abstract fun transferDetailDao(): TransferDetailDao
    abstract fun merchantDao(): MerchantDao
    abstract fun tellerDao(): TellerDao

    companion object {

        // To make sure there is always only one instance of the database open
        @Volatile
        private var INSTANCE: BitsyDatabase? = null

        internal fun getDatabase(context: Context): BitsyDatabase? {
            if (INSTANCE == null) {
                synchronized(BitsyDatabase::class.java) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        BitsyDatabase::class.java, "BiTSyWallet.db"
                    ).addMigrations(MIGRATION_1_2).build()
                }
            }

            return INSTANCE
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS 'merchants' ('id' TEXT NOT NULL PRIMARY KEY, 'name' TEXT NOT NULL, 'address' TEXT, 'lat' REAL NOT NULL, 'lon' REAL NOT NULL, 'phone' TEXT, 'telegram' TEXT, 'website' TEXT)")
                database.execSQL("CREATE TABLE IF NOT EXISTS 'tellers' ('id' TEXT NOT NULL PRIMARY KEY, 'name' TEXT NOT NULL, 'address' TEXT, 'lat' REAL NOT NULL, 'lon' REAL NOT NULL, 'phone' TEXT, 'telegram' TEXT, 'website' TEXT)")
            }
        }
    }
}
