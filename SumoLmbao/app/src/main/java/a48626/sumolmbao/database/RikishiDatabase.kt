package a48626.sumolmbao.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RikishiEntity::class], version = 18)
abstract class RikishiDatabase : RoomDatabase() {
    abstract fun rikishiDao(): RikishiDao

    companion object {
        @Volatile private var INSTANCE: RikishiDatabase? = null

        fun getDatabase(context: Context): RikishiDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    RikishiDatabase::class.java,
                    "rikishi_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
