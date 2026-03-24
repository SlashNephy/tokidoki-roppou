package blue.starry.tokidokiroppou.di

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// local フレーバーではデバッグ用の App Check プロバイダーを使用
@Module
@InstallIn(SingletonComponent::class)
internal object AppCheckProviderFactoryModule {
    @Provides
    @Singleton
    fun provide(): AppCheckProviderFactory {
        return DebugAppCheckProviderFactory.getInstance()
    }
}
