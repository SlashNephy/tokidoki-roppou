package blue.starry.tokidokiroppou.core.ai.di

import javax.inject.Qualifier

/** Google Search グラウンディング付きの GenerativeModel */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Grounded

/** グラウンディングなしの GenerativeModel (フォールバック用) */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Plain
