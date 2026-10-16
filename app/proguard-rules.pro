# Правила R8 для release-сборки. Firebase, Coil, Media3, WorkManager и Jsoup приносят свои правила сами.
# Если release-сборка вдруг упадёт с ClassNotFound или пропадёт часть данных форума, добавь сюда нужный класс:
# -keep class ru.huegrad.app.forum.** { *; }
-keepattributes Signature,*Annotation*
