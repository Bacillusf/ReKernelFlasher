package safe.kernel.flash.common.types.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import safe.kernel.flash.common.types.room.updates.Update
import safe.kernel.flash.common.types.room.updates.UpdateDao

@Database(entities = [Update::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun updateDao(): UpdateDao
}
