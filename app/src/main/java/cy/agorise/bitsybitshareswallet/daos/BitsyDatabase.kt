package cy.agorise.bitsybitshareswallet.daos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cy.agorise.bitsybitshareswallet.models.Asset

@Database(entities = [Asset::class], version = 1, exportSchema = false)
abstract class BitsyDatabase : RoomDatabase() {

    abstract fun assetDao(): AssetDao
    abstract fun balanceDao(): BalanceDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun authorityDao(): AuthorityDao

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

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
