package com.example.carniceriaapp20.di

import android.content.Context
import androidx.room.Room
import com.example.carniceriaapp20.data.local.CarniceriaDatabase
import com.example.carniceriaapp20.data.local.LabelHistoryDao
import com.example.carniceriaapp20.data.local.ProductDao
import com.example.carniceriaapp20.data.local.TicketDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): CarniceriaDatabase {
        return Room.databaseBuilder(
            appContext,
            CarniceriaDatabase::class.java,
            "carniceria_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideProductDao(database: CarniceriaDatabase): ProductDao {
        return database.productDao()
    }

    @Provides
    fun provideTicketDao(database: CarniceriaDatabase): TicketDao {
        return database.ticketDao()
    }

    @Provides
    fun provideLabelHistoryDao(database: CarniceriaDatabase): LabelHistoryDao { 
        return database.labelHistoryDao()
    }
}
