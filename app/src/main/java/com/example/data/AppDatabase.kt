package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LessonNote::class,
        MCQSet::class,
        TheorySet::class,
        TimetableItem::class,
        SyllabusItem::class,
        SchoolClass::class,
        Student::class,
        UserPreference::class,
        UserAccount::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun teacherDao(): TeacherDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teachers_companion_db"
                )
                .fallbackToDestructiveMigration() // safe default for client prototyping
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
