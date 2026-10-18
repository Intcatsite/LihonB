# Документация системы плагинов LihonB (.lbp)

**Lihon Browser Plugin (`.lbp`)** — модульная система расширения браузера LihonB на базе архитектуры Android `DexClassLoader` и нативного байткода Dalvik. Плагины позволяют безопасно изменять интерфейс браузера, перехватывать сетевые запросы, инжектировать скрипты/стили в веб-страницы и предоставлять пользователю собственные настройки в дизайне **Material You (MD3)**.

---

## 1. Структура плагина (.lbp)

Файл `.lbp` представляет собой обычный ZIP-архив, содержащий два ключевых файла:

```text
my_plugin.lbp (ZIP-архив)
├── manifest.json   # Метаданные плагина и точка входа
└── classes.dex     # Скомпилированный Android-байткод классов плагина
```

---

## 2. Манифест (`manifest.json`)

Манифест описывает идентификатор, автора, версию и точку входа плагина:

```json
{
  "id": "space.lihont.plugin.cyber",
  "name": "Cyber Home",
  "name_ru": "Кибер-Главный Экран",
  "version": "1.0",
  "author": "Lihon Mod Team",
  "description": "Кастомизирует главный экран, добавляет неоновый виджет и быстрые действия.",
  "entry": "space.lihont.plugin.cyber.CyberHomePlugin",
  "min_api": 26
}
```

### Описание полей:
| Поле | Тип | Описание |
| :--- | :--- | :--- |
| `id` | `String` | Уникальный ID плагина (package notation) |
| `name` | `String` | Название плагина на английском |
| `name_ru` | `String` | Локализованное название на русском (отображается в UI) |
| `version` | `String` | Версия плагина (например, `"1.0"`) |
| `author` | `String` | Имя создателя плагина |
| `description` | `String` | Краткое описание функционала |
| `entry` | `String` | Полное имя класса-наследника `LihonPlugin` |
| `min_api` | `int` | Минимальная версия Android API (рекомендуется 26+) |

---

## 3. Базовый класс плагина (`LihonPlugin`)

Каждый плагин наследуется от `space.lihont.browser.plugin.LihonPlugin` и переопределяет нужные методы:

```java
package space.lihont.plugin.cyber;

import android.app.Activity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebResourceResponse;
import space.lihont.browser.plugin.LihonPlugin;
import space.lihont.browser.plugin.PluginContext;

public class MyPlugin extends LihonPlugin {

    @Override
    public void onStart(PluginContext ctx) {
        // Вызывается при установке или включении плагина
        ctx.log("Плагин активирован!");
        
        // Регистрация авто-настроек Material 3:
        ctx.addSettingsSection("Основные настройки");
        ctx.addSettingsToggle("Показывать виджет", "Включает виджет на главном экране", "show_widget", true);
        ctx.addSettingsChoice("Цвет темы", "Выбор акцентного цвета", "accent_color", 
            new String[]{"Фиолетовый", "Зелёный", "Розовый"}, 0);
        ctx.addSettingsSlider("Размер шрифта", "Размер текста виджета в sp", "font_size", 12, 24, 16);
    }

    @Override
    public void onStop() {
        // Вызывается при выключении или удалении плагина
        // Очистите созданные виджеты и таймеры
    }

    @Override
    public void onSettingsChanged(String key) {
        // Вызывается автоматически при изменении любого параметра в окне настроек
    }

    @Override
    public void onHomeScreenCreated(Activity activity, View homeScreenRoot) {
        // Хук кастомизации главного экрана браузера!
        // homeScreenRoot содержит start_page_content.
        // Вы можете добавлять карточки, кнопки, менять фон и т.д.
    }

    @Override
    public void onPageStarted(WebView webView, String url) {
        // Вызывается перед началом загрузки страницы в веб-вкладке
    }

    @Override
    public void onPageFinished(WebView webView, String url) {
        // Вызывается после загрузки страницы (удобно для инжекции JS/CSS)
        getContext().injectCss("body { filter: contrast(105%); }");
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(String url) {
        // Перехват веб-запросов (блокировка рекламы, замена ресурсов, мокинг)
        // Верните null, чтобы пропустить запрос штатно
        return null;
    }
}
```

---

## 4. API контекста (`PluginContext`)

Через объект `getContext()` (или `ctx` в `onStart`) доступны следующие функции:

- **Изолированное хранилище данных**:
  ```java
  SharedPreferences prefs = ctx.prefs();
  boolean enabled = prefs.getBoolean("my_key", true);
  prefs.edit().putBoolean("my_key", false).apply();
  ```
- **Управление браузером**:
  ```java
  ctx.openUrl("https://lihont.space"); // Открыть сайт в новой вкладке
  ctx.showToast("Сообщение");          // Показать системное уведомление
  ctx.log("Текст в logcat");           // Логирование с тегом LihonPlugin
  ```
- **Инжекция скриптов и стилей в активную вкладку**:
  ```java
  ctx.injectJs("alert('LihonB Plugin loaded');");
  ctx.injectCss("body { background-color: #121212 !important; }");
  ```
- **Декларативные настройки (MD3 Settings)**:
  - `addSettingsSection(title)` — раздел заголовка.
  - `addSettingsToggle(title, desc, key, defVal)` — тумблер Вкл/Выкл.
  - `addSettingsChoice(title, desc, key, options[], defIndex)` — выпадающий выбор.
  - `addSettingsSlider(title, desc, key, min, max, defVal)` — ползунок со значениями.

Все зарегистрированные настройки **автоматически рендерятся** в диалоге плагина в дизайне Material Design 3 Expressive без написания XML-разметки!

---

## 5. Как собрать плагин (`.lbp`)

### Быстрая сборка (Python-скрипт):
В комплекте проекта есть готовый скрипт:
```bash
python scripts/build_sample_plugin.py
```

### Ручная сборка (шаги):
1. **Компиляция Java** (с `android.jar` и классами LihonB в classpath):
   ```bash
   javac --release 8 -cp "android.jar;LihonB/build/classes" -d build/classes src/**/*.java
   ```
2. **Конвертация в Dalvik байткод (`classes.dex`) через D8**:
   ```bash
   java -cp tools/r8.jar com.android.tools.r8.D8 --release --min-api 26 --output build/dex --lib android.jar --lib LihonB/build/classes build/classes/**/*.class
   ```
3. **Упаковка в ZIP `.lbp`**:
   ```bash
   zip -j my_plugin.lbp manifest.json build/dex/classes.dex
   ```

---

## 6. Установка и использование в LihonB

1. **Из меню браузера**:
   - Нажмите `⋮` (три точки в нижнем баре) → **🧩 Плагины (.lbp)**.
   - Нажмите **+ Установить**.
   - Доступен выбор готового демо-плагина **Cyber Mod**, автопоиск файлов `.lbp` в папке `Download` или выбор через системный проводник.
2. **Прямое открытие**:
   - Браузер зарегистрирован в системе для обработки файлов `.lbp`. Достаточно нажать на файл в Telegram, проводнике или браузере, и LihonB установит его в 1 клик.
3. **Управление**:
   - Каждый плагин можно выключить тумблером, настроить через кнопку **⚙ НАСТРОЙКИ** или удалить иконкой корзины.
