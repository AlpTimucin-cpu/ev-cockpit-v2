package app.evcockpit.di

import android.content.Context
import app.evcockpit.CockpitApplication
import app.evcockpit.location.LocationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Konum servisi ile mevcut ekranların tek bir GPS akışını paylaşmasını sağlar.
 * Önceden oluşturulan uygulama deposu korunur; ayrı StateFlow üretilmez.
 */
@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    @Provides
    @Singleton
    fun provideLocationRepository(@ApplicationContext context: Context): LocationRepository =
        (context.applicationContext as CockpitApplication).locationRepository
}
