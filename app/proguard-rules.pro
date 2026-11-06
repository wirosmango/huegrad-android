# Правила R8 для release-сборки. Firebase, Coil, Media3, WorkManager и Jsoup приносят свои правила сами.
# Если release-сборка вдруг упадёт с ClassNotFound или пропадёт часть данных форума, добавь сюда нужный класс:
# -keep class ru.huegrad.app.forum.** { *; }
-keepattributes Signature,*Annotation*

# WorkManager хранит очередь в Room и создаёт базу через рефлексию (getDeclaredConstructor()).
# Без этих правил R8 вырезает конструктор WorkDatabase_Impl, и приложение падает при старте
# (NoSuchMethodException: androidx.work.impl.WorkDatabase_Impl.<init> []).
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { <init>(); }
