package com.github.pockethub.android

import android.app.Application
import android.content.Context
import com.github.pockethub.android.Database.Companion.Schema
import com.squareup.sqldelight.android.AndroidSqliteDriver
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal abstract class ApplicationModule {
    @Binds
    @Singleton
    abstract fun provideApplicationContext(application: Application?): Context?

    companion object {
        @JvmStatic
        @Singleton
        @Provides
        fun provideDatabase(context: Context?): Database {
            val driver = AndroidSqliteDriver(Schema, context!!, "cache.db")
            return Database(driver)
        }
    }
}