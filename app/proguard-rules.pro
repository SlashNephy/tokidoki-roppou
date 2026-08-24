# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}

# Glance
# ActionCallback は Glance がリフレクションで引数なしコンストラクタを呼んで
# インスタンス化するため、R8 に削除されないよう保持する。
# 削除されると release ビルドでのみウィジェットのボタンが無反応になる
# (java.lang.NoSuchMethodException: ...<init> [])
-keep class * extends androidx.glance.appwidget.action.ActionCallback {
    <init>();
}
