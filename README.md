# E-Pay

Onlayn kompyuter do'koni. Spring Boot 4, Java 21, PostgreSQL. Batafsil qoidalar — [CLAUDE.md](CLAUDE.md).

## Ishga tushirish
Kerak: JDK 21. Maven o'rnatish shart emas — `mvnw` wrapper o'zi yuklab oladi.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
`dev` profil xotiradagi H2 bazasidan foydalanadi. Ilova: http://localhost:8080, holati: http://localhost:8080/actuator/health

`prod` profil uchun muhit o'zgaruvchilari: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.

## Testlar
```bash
./mvnw test     # unit va tezkor testlar
./mvnw verify   # to'liq tekshiruv (integratsion testlar uchun Docker kerak — Testcontainers)
```
Har bir PR va `main` ga push'da GitHub Actions `./mvnw -B verify` ni ishga tushiradi (`.github/workflows/ci.yml`).

## Branch qoidalari
`main` himoyalangan: to'g'ridan-to'g'ri push yo'q, faqat PR orqali va CI yashil bo'lganda.

| Prefiks | Nima uchun | Misol |
|---|---|---|
| `feature/` | yangi imkoniyat | `feature/payme-callback` |
| `fix/` | xatoni tuzatish | `fix/order-total-rounding` |
| `refactor/` | xatti-harakatni o'zgartirmaydigan qayta yozish | `refactor/order-service` |
| `docs/` | hujjatlar | `docs/readme-setup` |

## Commit xabarlari — Conventional Commits
Format: `<tur>(<ixtiyoriy soha>): <qisqa tavsif>`

- `feat:` — yangi imkoniyat
- `fix:` — xato tuzatish
- `refactor:` — qayta yozish
- `docs:` — hujjatlar
- `test:` — testlar
- `build:` / `ci:` — build va CI sozlamalari
- `chore:` — boshqa mayda ishlar

Misollar: `feat(payment): Click prepare callback`, `fix: so'mni HALF_UP bilan yaxlitlash`.
