# Нативное приложение сайта Хуеграда для Android
# !!AI Slop!! (если для вас это принципиально важно)

## Скриншоты

<img width="225" height="500" alt="5431712966387637370_119" src="https://github.com/user-attachments/assets/2ecd92bf-7c4f-466a-a08b-c9da642f8c08" />
<img width="225" height="500" alt="5431712966387637369_119" src="https://github.com/user-attachments/assets/ff80de4a-0f95-4afa-85b6-978249cfdf4c" />
<img width="225" height="500" alt="5431712966387637368_119" src="https://github.com/user-attachments/assets/93f54ba6-02c0-4305-872b-42e02a32d2d2" />
<img width="225" height="500" alt="5431712966387637367_119" src="https://github.com/user-attachments/assets/7363c795-727a-421b-afe8-0fd904eb4c99" />
<img width="225" height="500" alt="5431712966387637366_119" src="https://github.com/user-attachments/assets/628e803b-e9e1-4a41-8e48-b93869b0fc24" />

## Ручная сборка
1. `app/google-services.json` из Firebase Console (package `ru.huegrad.app`): скопируйте `app/google-services.json.example` и замените значениями своего проекта, либо скачайте файл из Firebase. Файл в репозиторий не попадает. Без него приложение собирается, форум покажет подсказку. Для входа через Google включите его в Authentication и добавьте SHA-1 и SHA-256 ключа подписи, затем скачайте файл заново.
2. В `forum/ForumViewModel.kt` сверить имя коллекции и поля с веб-форумом.
3. Перенесите треки в `app/src/main/res/raw/` (имя только строчными латиницей/цифрами) и при необходимости добавьте треки в `Content.tracks`.
4. Material 3 Expressive требует material3 1.5.0-alpha*, а он (как и Compose 1.12) требует AGP 9.1+, Gradle 9.3.1, JDK 17+ и compileSdk 37 (в Android Studio: SDK Manager, Android 37). Нужна свежая Android Studio, поддерживающая AGP 9.1.

## Подпись релиза
Ключ (`*.jks`) и `keystore.properties` в репозиторий не добавляются. Создайте свой ключ и файл `keystore.properties` в корне:

```
storeFile=C:/путь/к/ключу.jks
storePassword=...
keyAlias=...
keyPassword=...
```

## Шрифт
Используется Google Sans (Copyright The Google Sans Project Authors, github.com/googlefonts/googlesans), лицензия SIL Open Font License 1.1, текст в `app/src/main/assets/licenses/OFL.txt`. Файлы шрифта уменьшены до латиницы, кириллицы и основной пунктуации.
