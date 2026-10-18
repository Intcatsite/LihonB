# LihonB — Ультралёгкий приватный браузер в стиле Material Design 3 Expressive

[![License: MIT](https://img.shields.io/badge/License-MIT-purple.svg)](https://opensource.org/licenses/MIT)
[![APK Size](https://img.shields.io/badge/APK_Size-0.11_MB-success.svg)](https://lihont.space/lihonb-1.0-beta.apk)
[![Android](https://img.shields.io/badge/Android-8.0%2B-blue.svg)](https://lihont.space/lihonb)
[![Official Website](https://img.shields.io/badge/Website-lihont.space%2Flihonb-magenta.svg)](https://lihont.space/lihonb)

**LihonB** — это сверхбыстрый, минималистичный и бескомпромиссно приватный веб-браузер для Android.
В отличие от современных «комбайнов» (Chrome, Яндекс Браузер), раздутых до сотен мегабайт рекламных SDK, LihonB весит всего **110 КБ** и использует нативный системный движок **Android WebView**.

---

## ⚡ Ключевые преимущества перед обычными браузерами

| Характеристика | LihonB 1.0 Beta | Google Chrome | Яндекс Браузер |
| :--- | :---: | :---: | :---: |
| **Размер установочного APK** | ⚡ **0.11 МБ (110 КБ)** | ~185 МБ | ~240 МБ |
| **Встроенный AdBlock** | ✅ **Двухконтурный (сеть + DOM)** | ❌ Нет | ⚠️ Только лёгкий |
| **Расширения и плагины** | 🧩 **.LBP (DEX + UI)** | ❌ Заблокировано | ⚠️ Ограниченно |
| **Уничтожение данных в Инкогнито** | 🛡️ **100% очистка всех баз и кук** | ⚠️ Частично | ⚠️ Частично |
| **Фоновая телеметрия и трекеры** | 🚫 **0 трекеров, полная тишина** | ⚠️ Постоянная передача | ⚠️ Метрика, Crypta |
| **Оперативная память (RAM)** | 🚀 **~18 - 35 МБ** | ~200 - 450 МБ | ~250 - 600 МБ |
| **Назойливые ленты и пуши** | 🚫 **0 бурмалды** | ⚠️ Google Discover | ⚠️ Дзен, баннеры, курсы |
| **Лицензия** | 📄 **MIT (100% Open Source)** | Проприетарная | Проприетарная |

---

## 🛡️ Бескомпромиссная приватность (Wipe-on-Exit)
При выходе из режима инкогнито и закрытии приватных вкладок LihonB производит аппаратную очистку:
- `CookieManager` полностью сбрасывается и очищает сессионные куки.
- Базы `WebSQL`, `IndexedDB` и `LocalStorage` уничтожаются.
- Кэш страниц `WebView` удаляется с накопителя.
- История переходов и бэк-стек навигации моментально стираются.

---

## 🧩 Система плагинов .LBP (Lihon Browser Plugin)
LihonB поддерживает динамическую установку плагинов без пересборки APK.
Файл `.lbp` — это ZIP-архив с манифестом `plugin.json` и скомпилированным байткодом `classes.dex`:
- Изменение домашнего экрана и навигации.
- Инъекция кастомных стилей CSS (например, глобальная тёмная тема для всех сайтов с плагином `dark_pages.lbp`).
- Автоматическая генерация Material Design 3 диалогов настроек плагина.

Подробная документация: [PLUGINS_GUIDE.md](PLUGINS_GUIDE.md).

---

## 🛠️ Сборка из исходников

Для сборки проекта требуются **JDK 17+** и **Python 3.10+**:

```bash
git clone https://github.com/Intcatsite/LihonB.git
cd LihonB
python scripts/build_lihonb.py
```
Готовый подписанный APK появится в папке `ComplectAPK/lihonb-1.0-beta.apk`.

---

## 📄 Лицензия MIT
Проект распространяется под свободной лицензией MIT. Вы можете свободно использовать, модифицировать, форкать и распространять LihonB. Подробнее в файле [LICENSE](LICENSE).

---
© 2026 [cheb0chik](https://t.me/cheb0chik) • [lihont.space](https://lihont.space/lihonb)
