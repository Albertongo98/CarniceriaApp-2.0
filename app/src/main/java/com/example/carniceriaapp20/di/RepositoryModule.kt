package com.example.carniceriaapp20.di

import com.example.carniceriaapp20.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(productRepositoryImpl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindTicketRepository(ticketRepositoryImpl: TicketRepositoryImpl): TicketRepository

    @Binds
    @Singleton
    abstract fun bindLabelHistoryRepository(labelHistoryRepositoryImpl: LabelHistoryRepositoryImpl): LabelHistoryRepository
}
