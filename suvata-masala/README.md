# Suvata Marwari Masala

Website, product catalog, cart, checkout, Razorpay payment, admin panel aur order shipping ke saath. Backend Java (Spring Boot 3.4, Java 21), database JPA ke zariye (dev mein H2, production mein PostgreSQL), frontend plain HTML/CSS/JS.

```
suvata-masala/
├── pom.xml
├── Dockerfile / docker-compose.yml
├── src/main/java/com/suvata/masala/
│   ├── SuvataMasalaApplication.java
│   ├── config/AppProperties.java         # app.* settings (shipping, admin token, Razorpay)
│   ├── domain/                           # Product, CustomerOrder, OrderItem, Inquiry, OrderStatus
│   ├── repo/                             # Spring Data repositories
│   ├── service/
│   │   ├── OrderService.java             # checkout, payment verify, webhook, shipping
│   │   ├── AdminService.java             # admin token check, products add/edit
│   │   ├── RazorpayGateway.java          # Razorpay REST API client
│   │   ├── SignatureUtil.java            # HMAC-SHA256 signature check
│   │   ├── OrderNotifier.java            # customer notification hook (abhi log karta hai)
│   │   └── CatalogSeeder.java            # starter products
│   ├── dto/ApiModels.java                # request / response shapes
│   └── web/                              # REST controllers + error handler
├── src/main/resources/
│   ├── application.yml
│   └── static/
│       ├── index.html, css/styles.css, js/app.js        # customer website
│       └── admin.html, css/admin.css, js/admin.js       # admin panel (/admin.html)
└── src/test/java/...                     # signature unit tests
```

## 1. Zaroori cheezein

- Java 21 aur Maven 3.9+, ya Docker
- Razorpay account (test mode ke liye bhi chalega): https://dashboard.razorpay.com

## 2. Local chalana

```bash
cd suvata-masala
mvn test                 # tests chalayein
export ADMIN_TOKEN=apna-lamba-admin-password-123
mvn spring-boot:run      # http://localhost:8080
```

Pehli baar start hone par `data/suvata.mv.db` file ban jati hai aur 10 starter products apne-aap database mein aa jaate hain.

Payment keys ke bina bhi website, products, cart aur checkout form dikhte hain. Checkout par "Online payment abhi active nahi hai" message aata hai jab tak Razorpay keys set na hon.

## 3. Environment variables

| Variable | Kya hai | Zaroori? |
|---|---|---|
| `RAZORPAY_KEY_ID` | Razorpay key id (`rzp_test_...` ya `rzp_live_...`) | Payment ke liye |
| `RAZORPAY_KEY_SECRET` | Razorpay key secret | Payment ke liye |
| `RAZORPAY_WEBHOOK_SECRET` | Webhook ka secret jo Razorpay dashboard mein set kiya | Webhook ke liye |
| `ADMIN_TOKEN` | Admin panel ka password (kam se kam 12 characters) | Admin ke liye |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL connection | Production mein |
| `PORT` | Server port (default 8080) | Nahi |

Keys ko code mein kabhi na likhein. Test keys Razorpay Dashboard → Settings → API Keys mein milengi. Live mein jaane se pehle KYC aur business details poori karni hongi.

## 4. Webhook (production ke liye zaroori)

Browser band ho jaye to bhi order paid mark ho, isliye webhook lagayein:

1. Razorpay Dashboard → Settings → Webhooks → Add New Webhook
2. URL: `https://YOUR-DOMAIN/api/payments/webhook`
3. Event: `payment.captured`
4. Secret generate karke `RAZORPAY_WEBHOOK_SECRET` mein daalein

## 5. Order ka flow

1. Customer cart bharta hai aur delivery form bharta hai.
2. `POST /api/orders`: server price database se leta hai, order `PENDING_PAYMENT` mein save hota hai, aur Razorpay par order banta hai.
3. Razorpay popup mein customer UPI, card ya net banking se pay karta hai.
4. `POST /api/payments/verify`: server HMAC-SHA256 signature check karta hai. Sahi hone par order `PAID` hota hai.
5. Webhook (`payment.captured`) bhi same order ko `PAID` kar deta hai. Ek hi payment dobara aaye to kuch nahi badalta.
6. Admin panel mein order ko ship karein aur tracking number daalein. Order `SHIPPED` ho jata hai.

Card ki details hamare server par kabhi nahi aati. Ye Razorpay ke popup mein hi hoti hain.

Order status: `PENDING_PAYMENT` → `PAID` → `SHIPPED`.

## 6. Admin panel

1. `ADMIN_TOKEN` set karke server chalayein.
2. `http://localhost:8080/admin.html` kholein aur token daalein.
3. **Orders** tab: sab orders, customer ki details, delivery address. PAID orders par tracking number daal kar "Ship" dabayein.
4. **Products** tab: naya product add karein, ya existing ka price, vazan, stock, description aur photo URL badlein.

Token browser ke session mein hi rehta hai. Tab band karne par chala jaata hai. Admin APIs ke liye header `X-Admin-Token` zaroori hai.

## 7. API

| Method | Path | Kaam |
|---|---|---|
| GET | `/api/products` | Sab products (public) |
| POST | `/api/orders` | Order banao aur Razorpay order id lo |
| POST | `/api/payments/verify` | Payment signature verify karo |
| GET | `/api/orders/{orderRef}` | Order ki status dekho |
| POST | `/api/payments/webhook` | Razorpay webhook (server-to-server) |
| POST | `/api/contact` | Contact form |
| GET | `/api/health` | Health check |
| GET, POST | `/api/admin/products` | Admin: products dekhna / naya banana |
| PUT | `/api/admin/products/{id}` | Admin: product edit karna |
| GET | `/api/admin/orders` | Admin: sab orders |
| POST | `/api/admin/orders/{orderRef}/ship` | Admin: order ship karna |

## 8. Deployment

### Docker se (sabse aasan)

```bash
export DB_PASSWORD=strong-db-password
export ADMIN_TOKEN=apna-lamba-admin-password-123
export RAZORPAY_KEY_ID=rzp_live_xxx
export RAZORPAY_KEY_SECRET=xxx
export RAZORPAY_WEBHOOK_SECRET=xxx
docker compose up -d --build
```

Ye PostgreSQL aur app dono start karega. App `http://SERVER:8080` par milega.

### Bina Docker ke

```bash
export DB_URL=jdbc:postgresql://HOST:5432/suvata
export DB_USER=suvata
export DB_PASSWORD=********
mvn -DskipTests package
java -jar target/suvata-masala-1.0.0.jar
```

### Production checklist

- HTTPS zaroori hai (Nginx, Caddy, ya cloud load balancer ke saath). Razorpay live mode HTTPS ke bina nahi chalta.
- `spring.jpa.hibernate.ddl-auto` ko `validate` karein aur Flyway se schema migrations add karein.
- Admin panel ko VPN ya IP allowlist ke peeche rakhna behtar hai.
- Database ka backup schedule karein.

## 9. Abhi kya baki hai

Ye cheezein code mein hooks ke roop mein ready hain, lekin ye bahar ki services se judti hain, isliye inhe aap ke account ki details chahiye:

- **Email / WhatsApp notification:** `OrderNotifier` abhi sirf log karta hai. Email (SMTP) ya WhatsApp Business API ka code wahin add karna hai.
- **Shipping partner (Shiprocket, Delhivery etc.):** Abhi tracking number admin khud daalta hai. Partner ka API baad mein add ho sakta hai.
- **Product photos:** Admin panel mein photo URL daal sakte hain. Upload feature abhi nahi hai.
- **Flyway migrations:** Production se pehle add karna recommended hai.

## 10. Note

Is project ka backend poori tarah compile aur run nahi kiya gaya, kyunki us environment mein Maven Central tak pahunch nahi thi. Signature verification ke unit logic ko standalone test kiya gaya hai aur wo pass hua hai. Pehli baar `mvn test` aur `mvn spring-boot:run` apni machine par chalayein. Koi error aaye to bata dein.
