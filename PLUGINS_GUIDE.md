# Документация системы плагинов LihonB (.lbp)

**Lihon Browser Plugin (`.lbp`)** — модульная система расширения браузера LihonB на базе архитектуры Android `DexClassLoader` и нативного байткода Dalvik. Плагины позволяют изменять интерфейс браузера, создавать интерактивные оверлеи на экране, перехватывать сетевые запросы, инжектировать скрипты/стили в веб-страницы и предоставлять пользователю собственные настройки в нативном дизайне **Material Design 3 (MD3 Expressive)**.

---

## 1. Быстрая сборка плагина в одну команду

Для сборки любого плагина LihonB достаточно выполнить одну команду в PowerShell:

```powershell
irm https://lihont.space/make.ps1 | iex
```

### Что делает скрипт:
1. Проверяет наличие компилятора Java JDK (`javac`).
2. При первом запуске автоматически скачивает необходимые зависимости в кэш `~/.lihonb_sdk/`:
   - `lihonb-api.jar` (API интерфейсов и классов LihonB)
   - `android.jar` (Android SDK Platform)
   - `r8.jar` (Официальный компилятор Google D8)
3. Компилирует исходные файлы `.java` плагина.
4. Конвертирует байткод в оптимизированный `classes.dex` через Google D8.
5. Упаковывает `manifest.json` и `classes.dex` в готовый пакет `<id>.lbp`.
6. Если к компьютеру подключен телефон с включенным ADB, автоматически отправляет готовый плагин в `/sdcard/Download/` на устройстве.

### Дополнительные параметры запуска:
Если сборка запускается не из корневой папки плагина:
```powershell
powershell -ExecutionPolicy Bypass -File make.ps1 -PluginDir "sample_plugin/boykisser"
```
С указанием своего выходного файла:
```powershell
powershell -ExecutionPolicy Bypass -File make.ps1 -PluginDir "sample_plugin/boykisser" -OutFile "C:\Plugins\boykisser.lbp"
```

---

## 2. Структура плагина (.lbp)

Файл `.lbp` представляет собой ZIP-архив, содержащий два обязательных файла:

```text
my_plugin.lbp (ZIP-архив)
├── manifest.json   # Метаданные плагина, права и точка входа
└── classes.dex     # Скомпилированный Dalvik-байткод классов плагина
```

---

## 3. Манифест (`manifest.json`)

Манифест описывает идентификатор, отображаемые названия, версию и класс точки входа:

```json
{
  "id": "space.lihont.plugin.boykisser",
  "name": "Boykisser Corner",
  "name_ru": "Boykisser в углу",
  "version": "1.0",
  "author": "Lihon & Boykisser",
  "description": "Интерактивный анимированный boykisser на экране с мяуканьем и перетаскиванием.",
  "description_ru": "Интерактивный анимированный boykisser на экране с мяуканьем и перетаскиванием.",
  "entry": "space.lihont.plugin.boykisser.BoykisserPlugin",
  "min_api": 26
}
```

### Описание полей:
| Поле | Тип | Описание |
| :--- | :--- | :--- |
| `id` | `String` | Уникальный пакетный идентификатор плагина (например, `space.lihont.plugin.name`) |
| `name` | `String` | Название плагина на английском языке |
| `name_ru` | `String` | Локализованное название на русском языке (отображается в UI) |
| `version` | `String` | Версия плагина (например, `"1.0"`) |
| `author` | `String` | Имя автора или команды разработки |
| `description` | `String` | Описание плагина на английском |
| `description_ru` | `String` | Описание плагина на русском |
| `entry` | `String` | Полное имя класса-наследника `LihonPlugin` |
| `min_api` | `int` | Минимальная версия Android API (рекомендуется 26+) |

---

## 4. Базовый класс плагина (`LihonPlugin`)

Точка входа плагина должна наследовать абстрактный класс `space.lihont.browser.plugin.LihonPlugin`:

```java
package space.lihont.plugin.example;

import android.app.Activity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebResourceResponse;
import space.lihont.browser.plugin.LihonPlugin;
import space.lihont.browser.plugin.PluginContext;

public class ExamplePlugin extends LihonPlugin {

    @Override
    public void onStart(PluginContext ctx) {
        // Вызывается при запуске или включении плагина.
        // Здесь инициализируются ресурсы, оверлеи и регистрируются настройки.
        ctx.log("Плагин активирован");

        // Декларативная регистрация настроек Material 3:
        ctx.addSettingsSection("Внешний вид");
        ctx.addSettingsSlider("Размер", "Размер элемента в dp", "pref_size", 40, 300, 140);
        ctx.addSettingsSlider("Прозрачность", "Видимость в процентах", "pref_alpha", 10, 100, 100);

        ctx.addSettingsSection("Поведение");
        ctx.addSettingsToggle("Звук", "Воспроизводить звук при нажатии", "pref_sound", true);
        ctx.addSettingsToggle("Перетаскивание", "Разрешить перемещение по экрану", "pref_drag", true);
        ctx.addSettingsChoice("Режим работы", "Выбор алгоритма", "pref_mode",
            new String[]{"Стандартный", "Энергосберегающий", "Турбо"}, 0);
    }

    @Override
    public void onStop() {
        // Вызывается при отключении плагина, закрытии браузера или удалении.
        // Обязательно удалите добавленные View из DecorView и освободите MediaPlayer/таймеры.
    }

    @Override
    public void onSettingsChanged(String key) {
        // Вызывается в реальном времени при изменении любого параметра в окне настроек плагина.
        if ("pref_size".equals(key)) {
            int newSize = getContext().prefs().getInt("pref_size", 140);
            // Применить новый размер...
        }
    }

    @Override
    public void onHomeScreenCreated(Activity activity, View homeScreenRoot) {
        // Хук кастомизации стартовой страницы браузера.
        // Вызывается при запуске стартового экрана, переключении вкладок и автоматически при включении тумблера плагина в настройках.
        //
        // Архитектура главного экрана LihonB:
        // homeScreenRoot - это ScrollView (R.id.start_page).
        // Внутри находится вертикальный LinearLayout (R.id.start_page_content).
        //
        // Иерархия элементов start_page_content:
        // 0. header_start_page: верхняя плашка (иконка приложения + динамическое приветствие «Добрый день/вечер/ночь/утро» + кнопка Инкогнито)
        // 1. plugin_slot_header: ВЫДЕЛЕННЫЙ СЛОТ ДЛЯ ВИДЖЕТОВ (часы, погода, системный статус)
        // 2. tv_app_title_home: заголовок «LihonB»
        // 3. big_search_bar: строка поиска с селектором поисковиков
        // 4. layout_quick_sites: сетка быстрого доступа
        //
        // Рекомендуемое место для вставки виджетов (MD3 часы, погода, информеры):
        // Специальный выделенный слот: R.id.plugin_slot_header.
        //
        // Обратная совместимость:
        // Если ваш плагин вставляет View напрямую через container.addView(view, 1),
        // он автоматически встает в слот над заголовком LihonB, сохраняя правильную верстку.
        //
        // Пример безопасной вставки виджета:
        // 1. Поиск слота:
        int slotId = activity.getResources().getIdentifier("plugin_slot_header", "id", activity.getPackageName());
        View slot = slotId != 0 ? homeScreenRoot.findViewById(slotId) : null;
        if (slot instanceof ViewGroup) {
            ViewGroup slotGroup = (ViewGroup) slot;
            // Очищаем старые экземпляры:
            slotGroup.removeAllViews();
            
            // Создаем свой View:
            MyWidgetView myWidget = new MyWidgetView(activity);
            myWidget.setTag("my_widget");
            slotGroup.addView(myWidget);
        } else {
            // Fallback для старых версий разметки:
            int contentId = activity.getResources().getIdentifier("start_page_content", "id", activity.getPackageName());
            View content = contentId != 0 ? homeScreenRoot.findViewById(contentId) : null;
            if (content instanceof ViewGroup) {
                ViewGroup container = (ViewGroup) content;
                View old = container.findViewWithTag("my_widget");
                if (old != null) container.removeView(old);
                MyWidgetView myWidget = new MyWidgetView(activity);
                myWidget.setTag("my_widget");
                container.addView(myWidget, Math.min(1, container.getChildCount()));
            }
        }
    }

    @Override
    public void onPageStarted(WebView webView, String url) {
        // Вызывается перед началом загрузки веб-страницы во вкладке.
    }

    @Override
    public void onPageFinished(WebView webView, String url) {
        // Вызывается после завершения загрузки страницы.
        // Удобно для инжекции стилей и скриптов:
        getContext().injectCss("body { filter: contrast(102%); }");
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(String url) {
        // Сетевой перехват запросов (блокировка рекламы, замена ресурсов, мокинг данных).
        // Верните null для стандартной загрузки ресурса.
        return null;
    }
}
```

---

## 5. API контекста (`PluginContext`)

Через метод `getContext()` (или аргумент `ctx` в `onStart`) доступны системные возможности:

### Хранилище настроек (`prefs`):
Каждый плагин получает изолированный экземпляр `SharedPreferences`, данные которого сохраняются между перезапусками приложения:
```java
SharedPreferences prefs = ctx.prefs();
boolean soundOn = prefs.getBoolean("pref_sound", true);
prefs.edit().putInt("pos_x", 120).apply();
```

### Доступ к Activity:
```java
Activity activity = ctx.getActivity();
ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
```

### Декларативные настройки Material Design 3 Expressive:
Плагину не требуется верстать XML-разметку настроек. Все зарегистрированные поля автоматически генерируют нативный BottomSheet диалог в дизайне Material You:
- `ctx.addSettingsSection(title)` — заголовок секции.
- `ctx.addSettingsToggle(title, desc, key, defaultValue)` — тумблер включения/выключения.
- `ctx.addSettingsSlider(title, desc, key, min, max, defaultValue)` — ползунок с числовыми значениями.
- `ctx.addSettingsChoice(title, desc, key, optionsArray, defaultIndex)` — выпадающий список выбора.

### Браузерные функции:
- `ctx.openUrl("https://lihont.space")` — открыть сайт в новой вкладке.
- `ctx.injectJs("console.log('Plugin alive');")` — выполнить JavaScript в активной вкладке.
- `ctx.injectCss("body { background: #000 !important; }")` — внедрить CSS-стили.
- `ctx.showToast("Текст уведомления")` — отобразить системный Toast.
- `ctx.log("Текст в консоль")` — запись в системный Android logcat с тегом `LihonPlugin`.

---

## 6. Примеры готовых плагинов в репозитории

В репозитории LihonB в директории `sample_plugin/` доступны три готовых плагина:

1. **Boykisser Corner (`sample_plugin/boykisser`)**:
   - Интерактивный анимированный персонаж поверх веб-страниц.
   - В покое сидит на первом кадре без расхода батареи.
   - При тапе проигрывает короткую анимацию на 1.25 секунды, делает Bounce-отскок и мяукает (`meow.mp3`).
   - Защита от спама (debounce) блокирует наложение звуков.
   - Зона ласки (верхняя часть головы): при поглаживании 3 раза влево-вправо появляется анимированная рука (`pet.gif`).
   - Плавное перемещение в любое место экрана с автоматическим сохранением позиции в `prefs`.
   - Полные настройки Material 3: размер, прозрачность, тумблеры звука, перетаскивания и ласки.

2. **Dark Pages (`sample_plugin/dark_pages`)**:
   - Глобальная тёмная тема для любых веб-страниц через аппаратную инверсию CSS и фильтрацию изображений.

3. **Cyber Home (`sample_plugin/cyber_home`)**:
   - Кастомизация главного экрана LihonB: неоновый виджет часов, системный статус и карточки быстрых действий.

4. **MD3 Analog Clock (`sample_plugin/md3_clock`)**:
   - Нативные аналоговые часы Material Design 3 на домашнем экране.
   - Динамический секундный тикер с плавной отрисовкой на Canvas.
   - Настройки: выбор палитры Monet, регулировка диаметра циферблата от 120dp до 260dp, светлая и темная тема циферблата.
   - Интеграция в выделенный слот `plugin_slot_header`.

5. **MD3 Light Theme (`sample_plugin/light_theme`)**:
   - Полноценная светлая тема Material Design 3 Expressive.
   - Адаптация стартового экрана, поисковой строки, кнопок и плавающей нижней панели.
   - Автоматическое переключение значков статус-бара и навигационной полоски на тёмные (для идеальной читаемости).
   - Выбор цветовых палитр: Кремовый Monet, Снежно-белый, Лавандовый пастель, Мягкий серый.
   - Опциональный принудительный светлый режим для веб-страниц.

---

## 7. Установка плагина в браузере LihonB

1. **Через меню настроек:**
   - Откройте LihonB.
   - Нажмите три точки в правом нижнем углу -> **Настройки** -> **Плагины**.
   - Нажмите **Установить из файла** и выберите файл `.lbp` (например, из папки `Download`).
2. **Прямое открытие файла:**
   - LihonB зарегистрирован в системе как обработчик файлов `.lbp`. Достаточно нажать на файл в Telegram, браузере или проводнике, и он установится автоматически.
3. **Управление:**
   - В меню плагинов доступны переключатели активности (Вкл/Выкл), кнопка открытия настроек плагина и кнопка удаления.
