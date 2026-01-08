## Описание

**AINotes** объединяет чат с языковой моделью (локально в LM Studio или через API-провайдер с Bearer‑токеном) и систему ведения заметок на базе Realm.

Пользователь может:

* Вести диалог с ассистентом

  <p align="left">
    <img src="https://github.com/user-attachments/assets/d80bda05-1305-40e3-b8d1-b6fea20fe415" alt="AINotes Screenshot" width="300"/>
  </p>
* Получать ответы в режиме потоковой передачи (streaming)
* Сохранять фрагменты переписки как заметки

  <div style="display: flex; gap: 16px; align-items: flex-start;">
  <img src="https://github.com/user-attachments/assets/553d304d-be53-4c7c-a0ad-5c8fe725759f" alt="AINotes Screenshot" width="300"/>
  <img src="https://github.com/user-attachments/assets/3a8eff49-d040-41bb-bdba-003b1710fbd4" alt="AINotes Screenshot" width="300"/>

</div>
* Просматривать, редактировать и удалять ранее созданные заметки
  <p align="left">
    <img src="https://github.com/user-attachments/assets/4dfa35aa-84cc-4da3-9d65-9cacf87444aa" alt="AINotes Screenshot" width="300"/>
  </p>

---

## Основные возможности

### 1. Интерактивный чат

* Поддержка системных подсказок и переключение моделей (gemma-1b, gemma3-4b, gemma3-12b)
* Потоковая генерация ответов с постепенным обновлением UI
* Остановка генерации по нажатию кнопки
* Автопрокрутка к последнему сообщению (с учётом пользовательского скролла)
* Предустановленные «быстрые» подсказки (например, «Написать код», «Написать историю» и т.д.)

### 2. Менеджер API‑ключа

* Сохранение/чтение API‑ключа в SharedPreferences
* Возможность работы без API‑ключа (для локального запуска моделей в LM Studio)

### 3. Динамическое конфигурирование базового URL

* Interceptor для подмены URL на лету через EncryptedSharedPreferences
* Автообновление публичного адреса из ngrok API

### 4. Локальное хранилище на Realm

* Сущности `ChatMessageEntity` для сохранения истории переписки
* Сущности `Note` для хранения заголовков и содержимого заметок
* Репозитории и ViewModel для работы с базой (MVVM + Hilt)

### 5. Управление темной темой

* Сохранение выбора в DataStore
* Автоматическая установка режима до старта Activity

---

## Технологии и зависимости

* **Язык:** Kotlin
* **UI:** Jetpack Compose
* **DI:** Hilt
* **Сеть:** Retrofit + OkHttp + Interceptors (Auth, BaseUrl)
* **База данных:** Realm (RealmHelper)
* **Настройки:** DataStore (Preferences), EncryptedSharedPreferences
* **Асинхронность:** Coroutines, Channels, StateFlow
* **Прочее:** SplashScreen API, AppCompatDelegate для ночного режима
