package cy.agorise.bitsybitshareswallet.daos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cy.agorise.bitsybitshareswallet.models.*

@Database(entities = [
    Asset::class,
    Authority::class,
    Balance::class,
    EquivalentValue::class,
    Operation::class,
    Transfer::class,
    UserAccount::class
    ], version = 1, exportSchema = false)
abstract class BitsyDatabase : RoomDatabase() {

    abstract fun assetDao(): AssetDao
    abstract fun authorityDao(): AuthorityDao
    abstract fun balanceDao(): BalanceDao
    abstract fun equivalentValueDao(): EquivalentValueDao
    abstract fun operationDao(): OperationDao
    abstract fun transferDao(): TransferDao
    abstract fun userAccountDao(): UserAccountDao

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
                    )
                        .build()
                }
            }

            return INSTANCE
        }
    }
}
