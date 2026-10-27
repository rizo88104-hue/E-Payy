# E-Pay — onlayn kompyuter do'koni

## Stek
- Spring Boot 4.1.2 (Spring Security 7, Hibernate 7, Jackson 3), Java 21, Maven
- PostgreSQL 17 — asosiy baza; sxema Flyway orqali
- H2 (PostgreSQL rejimida) — faqat `dev` profil va tezkor testlar uchun
- Testcontainers (PostgreSQL) — integratsion testlar
- Frontend — build'siz HTML/CSS/JS, `src/main/resources/static` ichida

## Paketlar (`uz.epay`, TZ 3-bo'lim)
| Paket | Vazifasi |
|---|---|
| `config` | Spring konfiguratsiyalari (Security, CORS, Jackson, scheduler) |
| `controller` | REST endpoint'lar — faqat qabul qilish, validatsiya, javob |
| `service` | Biznes mantiq, tranzaksiyalar |
| `payment` | To'lov tizimlari (Payme, Click) integratsiyasi |
| `notification` | Xabarnomalar (SMS, e-mail, Telegram) |
| `repository` | Spring Data JPA repository'lar |
| `entity` | JPA entity'lar |
| `dto` | So'rov/javob DTO'lari (`record`) |
| `exception` | Maxsus xatolar va global handler |

## Buyruqlar
```bash
./mvnw test                                            # unit va tezkor testlar
./mvnw verify                                          # to'liq tekshiruv (integratsion testlar bilan)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  # lokal ishga tushirish (H2)
```
Profillar: `dev` (standart, H2) va `prod` (PostgreSQL, sozlamalar muhit o'zgaruvchilaridan).

## Arxitektura qoidalari
- Qatlamlar: Controller -> Service -> Repository. Qatlamdan sakrab o'tilmaydi.
- Controller'da biznes mantiq yo'q.
- Entity API'dan tashqariga chiqmaydi — faqat DTO (`record`).
- Xatolar RFC 9457 `ProblemDetail` formatida qaytariladi (global `@RestControllerAdvice`).

## Pul qoidalari
- Narx dollarda: bazada `NUMERIC(14,2)`, Java'da `BigDecimal`.
- So'mdagi summa buyurtma paytida `usd_rate` bo'yicha hisoblanadi, `RoundingMode.HALF_UP` bilan butun so'mga yaxlitlanadi va buyurtmada `BIGINT` sifatida qotiriladi (kurs keyin o'zgarsa ham o'zgarmaydi).
- Payme'ga summa tiyinda yuboriladi: so'm × 100.
- `double`/`float` pul uchun hech qachon ishlatilmaydi.

## Vaqt
- Bazada `timestamptz`, saqlash UTC'da (`hibernate.jdbc.time_zone: UTC`).
- "Kunlik" hisob-kitoblar (hisobotlar, limitlar, kun chegaralari) `Asia/Tashkent` bo'yicha.

## Belgilangan nomlar
- Muddati o'tgan buyurtmalarni yopuvchi job: `OrderExpiryJob`
- Health-check: `/actuator/health` (tafsilotlar faqat avtorizatsiyadan keyin)
- To'lov callback'lari:
  - `POST /api/payments/payme`
  - `POST /api/payments/click/prepare`
  - `POST /api/payments/click/complete`

## Ma'lumotlar bazasi sxemasi
- Sxema faqat yangi Flyway fayl bilan o'zgaradi: `src/main/resources/db/migration/V{n}__tavsif.sql`.
- Yozilgan (commit qilingan) migratsiya hech qachon o'zgartirilmaydi — tuzatish ham yangi `V{n+1}` fayl bilan.
- `ddl-auto: validate` — Hibernate sxemani yaratmaydi, faqat tekshiradi.

## Xavfsizlik
- Huquq (rol, egalik) har doim serverda tekshiriladi; frontendga ishonilmaydi.
- Narx va summa faqat serverda hisoblanadi; mijozdan kelgan narx e'tiborga olinmaydi.
- Kalitlar, parollar, token'lar faqat muhit o'zgaruvchilarida — kodda va repoda yo'q (`.env`, `application-local.yml` gitignore'da).
- CORS faqat o'z domenimiz uchun ochiq.

## Eski loyihadan ko'chirish
Namuna: `D:\test\E-payy` (Spring Boot 3.3.5, Java 17, paket `uz.pcstore`) — faqat o'qish uchun, o'zgartirilmaydi.
- Kod so'zma-so'z emas, Spring Boot 4 ga moslab ko'chiriladi:
  - `AntPathRequestMatcher` o'rniga `PathPatternRequestMatcher`
  - Jackson 3 paketlari (`tools.jackson.*`; annotatsiyalar `com.fasterxml.jackson.annotation` da qoladi)
  - Spring Security 7 / Hibernate 7 dagi o'zgarishlarga e'tibor berish
- Paket `uz.pcstore` -> `uz.epay`, yuqoridagi qoidalarga moslab.

## Ish tartibi
- Har bir o'zgarish test bilan birga keladi; `./mvnw test` o'tishi shart.
- Commit va push'ni foydalanuvchi o'zi qiladi (yoki aniq ruxsat berilganda).
