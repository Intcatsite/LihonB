# LihonB - Легковесный приватный браузер в стиле Material Design 3 Expressive

[![License: MIT](https://img.shields.io/badge/License-MIT-purple.svg)](https://opensource.org/licenses/MIT)
[![APK Size](https://img.shields.io/badge/APK_Size-0.31_MB-success.svg)](https://lihont.space/lihonb-1.0-beta.apk)
[![Android](https://img.shields.io/badge/Android-8.0%2B-blue.svg)](https://lihont.space/lihonb)
[![Official Website](https://img.shields.io/badge/Website-lihont.space%2Flihonb-magenta.svg)](https://lihont.space/lihonb)

LihonB - это быстрый, минималистичный и приватный веб-браузер для Android.
В отличие от тяжелых браузеров, раздутых до сотен мегабайт, LihonB весит менее 1 МБ и использует нативный движок Android WebView.

---

## Преимущества

| Характеристика | LihonB 1.0 Beta | Google Chrome | Яндекс Браузер |
| :--- | :---: | :---: | :---: |
| Размер установочного APK | 0.31 МБ | ~185 МБ | ~240 МБ |
| Встроенный AdBlock | Двухконтурный (сеть + DOM) | Нет | Ограниченно |
| Расширения и плагины | .LBP (DEX + UI) | Заблокировано | Ограниченно |
| Очистка в Инкогнито | 100% удаление кук и баз | Частично | Частично |
| Фоновая телеметрия | 0 трекеров | Постоянная передача | Метрика, Crypta |
| Оперативная память | ~20 - 45 МБ | ~200 - 450 МБ | ~250 - 600 МБ |
| Лицензия | MIT (Open Source) | Проприетарная | Проприетарная |

---

## Приватность (Wipe-on-Exit)
При выходе из режима инкогнито и закрытии приватных вкладок LihonB производит полную очистку:
- CookieManager сбрасывается и удаляет все куки.
- Базы WebSQL, IndexedDB и LocalStorage уничтожаются.
- Кэш страниц WebView удаляется.
- История переходов и бэк-стек навигации моментально очищаются.

---

## Система плагинов .LBP (Lihon Browser Plugin)
LihonB поддерживает динамическую загрузку плагинов без пересборки APK.
Файл .lbp - это ZIP-архив с манифестом manifest.json и скомпилированным байткодом classes.dex:
- Кастомизация домашнего экрана и размещение виджетов.
- Инъекция кастомных стилей CSS (например, темная тема для всех сайтов с dark_pages.lbp).
- Автоматическая генерация диалогов настроек плагина Material Design 3.

Подробная документация: [PLUGINS_GUIDE.md](PLUGINS_GUIDE.md).

---

## Сборка из исходников

Для сборки проекта требуются JDK 8+ и Python 3.10+:

```bash
git clone https://github.com/Intcatsite/LihonB.git
cd LihonB
python scripts/build_lihonb.py
```
Готовый подписанный APK появится в папке ComplectAPK/lihonb-1.0-beta.apk.

---

## Лицензия MIT
Проект распространяется под свободной лицензией MIT. Подробнее в файле LICENSE.

---
(c) 2026 Lihon Team - https://lihont.space/lihonb
