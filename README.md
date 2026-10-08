# Vtt2Srt — محوّل ترجمة VTT إلى SRT مع ترجمة عربية

تطبيق أندرويد (Java / MVVM) يختار ملف `.vtt` ويحوّله إلى `.srt` مع ترجمة النص إلى العربية أونلاين.

## التشغيل
1. افتح المجلد في Android Studio (Koala أو أحدث) ثم Sync.
2. شغّل على جهاز أو محاكي (minSdk 24).

## البنية
- `domain/` منطق نقي بلا اعتماد على أندرويد: parser, writer, translate, usecase
- `data/` قراءة/كتابة الملفات (SAF) وطلبات HTTP
- `ui/` Activity + ViewModel (LiveData) + Adapter

## أنماط التصميم
Strategy (`Translator`)، Decorator (`CachingTranslator`, `RetryingTranslator`)،
Chain of Responsibility (`FallbackTranslator`)، Factory (`TranslatorFactory`)،
Builder (`ConversionOptions`)، Observer (LiveData).

## ملاحظة
محرك الترجمة الأساسي هو واجهة Google Translate المجانية غير الرسمية، والاحتياطي MyMemory.
للإنتاج استبدلهما بـ Cloud Translation API بتنفيذ جديد لواجهة `Translator`.
