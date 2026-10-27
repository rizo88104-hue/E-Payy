package uz.epay.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.epay.config.EpayProperties;
import uz.epay.config.RateLimiter;
import uz.epay.dto.OrderDetails;
import uz.epay.dto.OrderPlacedResponse;
import uz.epay.dto.OrderRequest;
import uz.epay.dto.OrderSummary;
import uz.epay.dto.OrderTrackResponse;
import uz.epay.dto.PageResponse;
import uz.epay.entity.Order;
import uz.epay.entity.OrderItem;
import uz.epay.entity.OrderStatus;
import uz.epay.entity.OrderTransitions;
import uz.epay.entity.PaymentMethod;
import uz.epay.entity.PaymentStatus;
import uz.epay.entity.Product;
import uz.epay.entity.ProductStatus;
import uz.epay.entity.Staff;
import uz.epay.entity.StaffRole;
import uz.epay.exception.BadRequestException;
import uz.epay.exception.ConflictException;
import uz.epay.exception.ForbiddenException;
import uz.epay.exception.NotFoundException;
import uz.epay.repository.OrderRepository;
import uz.epay.repository.OrderSpecs;
import uz.epay.repository.ProductRepository;
import uz.epay.repository.StaffRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Buyurtmalar: xaridor yaratadi va kuzatadi; xodimlar ishlaydi.
 * Narx, kafolat, kurs va so'mdagi summa — faqat shu yerda (serverda) hisoblanadi va buyurtmada qotiriladi.
 * Holat o'tishlari — OrderTransitions.
 */
@Service
public class OrderService {
    /** "Kunlik" hisoblar va buyurtma raqamidagi yil — Toshkent vaqti bo'yicha. */
    public static final ZoneId TASHKENT = ZoneId.of("Asia/Tashkent");
    static final int MAX_LINES = 20;
    static final int MAX_QTY = 10;
    static final String NOT_FOUND = "Buyurtma topilmadi";
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final OrderRepository orders;
    private final ProductRepository products;
    private final StaffRepository staff;
    private final StockService stock;
    private final RateLimiter limiter;
    private final ApplicationEventPublisher events;
    private final EpayProperties props;

    public OrderService(OrderRepository orders, ProductRepository products, StaffRepository staff, StockService stock,
                        RateLimiter limiter, ApplicationEventPublisher events, EpayProperties props) {
        this.orders = orders;
        this.products = products;
        this.staff = staff;
        this.stock = stock;
        this.limiter = limiter;
        this.events = events;
        this.props = props;
    }

    // --- xaridor ---

    @Transactional
    public OrderPlacedResponse create(OrderRequest req, String clientIp) {
        limiter.check(RateLimiter.Limit.ORDER, clientIp);
        String phone = PhoneNumbers.normalize(req.phone());
        List<OrderRequest.Item> lines = mergeLines(req.items());
        int maxPending = props.order().maxPendingPerPhone();
        if (orders.countByPhoneAndStatusAndPayStatus(phone, OrderStatus.NEW, PaymentStatus.UNPAID) >= maxPending) {
            throw new ConflictException("Bu telefondan to'lanmagan buyurtmalar ko'p (" + maxPending
                    + " ta). Operator qo'ng'irog'ini kuting yoki ularni to'lang.");
        }
        BigDecimal rate = usdRate();

        Order order = new Order();
        order.setCustomerName(req.customerName().trim());
        order.setPhone(phone);
        order.setAddress(req.address().trim());
        order.setRegion(req.region());
        order.setPaymentMethod(req.paymentMethod() == null ? PaymentMethod.CASH : req.paymentMethod());

        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (OrderRequest.Item line : lines) {
            Product product = products.findById(line.productId())
                    .filter(p -> p.getStatus() == ProductStatus.PUBLISHED)
                    .orElseThrow(() -> new BadRequestException("Mahsulot sotuvda yo'q: " + line.productId()));
            int years = line.warrantyYears() == null || line.warrantyYears() == 0
                    ? baseWarranty() : line.warrantyYears();

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setSku(product.getSku());
            item.setUnitPriceUsd(priceWithWarranty(product.getPrice(), years));
            item.setWarrantyYears(years);
            item.setQuantity(line.quantity());
            item.setMxikCode(product.getMxikCode());
            item.setPackageCode(product.getPackageCode());
            order.addItem(item);
            total = total.add(item.lineTotalUsd());
        }

        // pul buyurtma paytida qotiriladi: kurs va so'mdagi summa keyin o'zgarmaydi
        order.setTotalUsd(total);
        order.setUsdRate(rate);
        order.setAmountUzs(Money.toUzs(total, rate));

        Instant now = Instant.now();
        order.setCreatedAt(now);
        order.setExpiresAt(now.plus(props.order().unpaidTimeout()));
        order.setNumber(String.format("EP-%d-%06d", LocalDate.ofInstant(now, TASHKENT).getYear(), orders.nextNumber()));

        // TODO(2.1): StockService haqiqiy realizatsiyasi — qoldiq yetmasa shu yerda BadRequest va tranzaksiya bekor
        stock.reserve(order);

        Order saved = orders.save(order);
        events.publishEvent(OrderEvent.of(OrderEvent.Type.CREATED, saved));
        return OrderPlacedResponse.from(saved);
    }

    /** Raqam va telefon mos kelsa. Aks holda (yo'q, telefon boshqa, format xato) — har doim bir xil 404. */
    @Transactional(readOnly = true)
    public OrderTrackResponse track(String number, String phone, String clientIp) {
        limiter.check(RateLimiter.Limit.TRACK, clientIp);
        String key = PhoneNumbers.key(phone);
        return orders.findByNumber(number.trim().toUpperCase())
                .filter(o -> key.length() == 9 && key.equals(PhoneNumbers.key(o.getPhone())))
                .map(OrderTrackResponse::from)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    /** Bir xil mahsulot qatorlari birlashtiriladi (kafolat — birinchisidan), keyin miqdor cheklovlari. */
    static List<OrderRequest.Item> mergeLines(List<OrderRequest.Item> items) {
        Map<Long, OrderRequest.Item> merged = new LinkedHashMap<>();
        for (OrderRequest.Item i : items) {
            merged.merge(i.productId(), i,
                    (a, b) -> new OrderRequest.Item(a.productId(), a.quantity() + b.quantity(), a.warrantyYears()));
        }
        if (merged.size() > MAX_LINES) {
            throw new BadRequestException("Bitta buyurtmada ko'pi bilan " + MAX_LINES + " xil mahsulot bo'lishi mumkin");
        }
        for (OrderRequest.Item i : merged.values()) {
            if (i.quantity() > MAX_QTY) {
                throw new BadRequestException("Bitta mahsulotdan ko'pi bilan " + MAX_QTY + " ta buyurtma qilish mumkin");
            }
        }
        return List.copyOf(merged.values());
    }

    // --- narx ---

    /** Eng qisqa kafolat muddati — narx ichida; muddat tanlanmasa shu olinadi. */
    int baseWarranty() {
        return props.order().warranty().keySet().iterator().next();
    }

    /** Narx + tanlangan kafolat muddati foizi, sentgacha HALF_UP. */
    BigDecimal priceWithWarranty(BigDecimal price, int years) {
        Integer percent = props.order().warranty().get(years);
        if (percent == null) {
            throw new BadRequestException("Kafolat muddati noto'g'ri: " + years + " yil. Mavjud: "
                    + props.order().warranty().keySet() + " yil");
        }
        return price.multiply(BigDecimal.valueOf(100L + percent)).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal usdRate() {
        BigDecimal rate = props.payment().usdRate();
        if (rate == null || rate.signum() <= 0) {
            throw new IllegalStateException("epay.payment.usd-rate (PAYMENT_USD_RATE) sozlanmagan");
        }
        return rate;
    }

    // --- xodimlar ---

    /** Operator faqat biriktirilmagan va o'zinikilarni ko'radi; ADMIN va OWNER — hammasini. */
    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> list(OrderStatus status, PaymentStatus payStatus, LocalDate from, LocalDate to,
                                           String q, int page, int size) {
        Long visibleTo = isOperatorOnly() ? me().getId() : null;
        Instant fromInstant = from == null ? null : from.atStartOfDay(TASHKENT).toInstant();
        // "to" kuni ham kiradi: keyingi kun boshigacha
        Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(TASHKENT).toInstant();
        return PageResponse.of(orders.findAll(OrderSpecs.filter(status, payStatus, fromInstant, toInstant, q, visibleTo),
                PageRequest.of(page, size, NEWEST_FIRST)), OrderSummary::from);
    }

    @Transactional(readOnly = true)
    public OrderDetails details(Long id) {
        Order o = load(id);
        requireVisible(o);
        return OrderDetails.from(o);
    }

    /** Operator biriktirilmagan buyurtmani o'ziga oladi. */
    @Transactional
    public OrderDetails take(Long id) {
        Order o = load(id);
        requireVisible(o);
        Staff me = me();
        if (o.getAssignedOperator() != null && !o.getAssignedOperator().getId().equals(me.getId())) {
            throw new ConflictException("Buyurtma boshqa xodimga biriktirilgan");
        }
        o.setAssignedOperator(me);
        return flush(o);
    }

    /** Faqat ADMIN: istalgan faol operatorga biriktiradi (yoki qayta biriktiradi). */
    @Transactional
    public OrderDetails assign(Long id, Long operatorId) {
        if (!CurrentUser.hasRole(StaffRole.ADMIN)) {
            throw new ForbiddenException("Boshqa xodimga biriktirish — faqat administrator");
        }
        Order o = load(id);
        Staff operator = staff.findById(operatorId)
                .filter(s -> s.isActive() && (s.getRole() == StaffRole.OPERATOR || s.getRole() == StaffRole.ADMIN))
                .orElseThrow(() -> new BadRequestException("Faol operator topilmadi: " + operatorId));
        o.setAssignedOperator(operator);
        return flush(o);
    }

    @Transactional
    public OrderDetails changeStatus(Long id, OrderStatus to) {
        Order o = load(id);
        claimIfOperator(o);
        OrderStatus from = o.getStatus();
        if (from == to) {
            return OrderDetails.from(o);
        }
        if (!OrderTransitions.canMove(from, to)) {
            throw new BadRequestException("Holatni o'zgartirib bo'lmaydi: " + from + " → " + to);
        }
        o.setStatus(to);
        switch (to) {
            case CONFIRMED -> events.publishEvent(OrderEvent.of(OrderEvent.Type.CONFIRMED, o));
            case CANCELLED -> {
                // to'langan buyurtma bekor qilinsa — pul qaytarilishi kerak (admin keyin REFUNDED qiladi)
                if (o.getPayStatus() == PaymentStatus.PAID) {
                    movePayment(o, PaymentStatus.REFUND_PENDING);
                }
                stock.release(o);
                events.publishEvent(OrderEvent.of(OrderEvent.Type.CANCELLED, o));
            }
            default -> {
            }
        }
        return flush(o);
    }

    @Transactional
    public OrderDetails note(Long id, String note) {
        Order o = load(id);
        claimIfOperator(o);
        o.setOperatorNote(note == null || note.isBlank() ? null : note.trim());
        return flush(o);
    }

    /** Pul qaytarildi (REFUND_PENDING → REFUNDED). Faqat ADMIN — SecurityConfig. */
    @Transactional
    public OrderDetails markRefunded(Long id) {
        Order o = load(id);
        movePayment(o, PaymentStatus.REFUNDED);
        return flush(o);
    }

    /**
     * To'lov qabul qilindi (Payme/Click callback'lari chaqiradi — keyingi bosqich). Bekor qilingan buyurtma
     * to'lanmaydi; takroriy chaqiruv (allaqachon PAID) — hech narsa qilmaydi.
     */
    @Transactional
    public void markPaid(Long id) {
        Order o = load(id);
        if (o.getPayStatus() == PaymentStatus.PAID) {
            return;
        }
        if (o.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Bekor qilingan buyurtmani to'lab bo'lmaydi: " + o.getNumber());
        }
        movePayment(o, PaymentStatus.PAID);
        o.setPaidAt(Instant.now());
        events.publishEvent(OrderEvent.of(OrderEvent.Type.PAID, o));
        orders.flush();
    }

    // --- yordamchilar ---

    private static void movePayment(Order o, PaymentStatus to) {
        if (!OrderTransitions.canMove(o.getPayStatus(), to)) {
            throw new BadRequestException("To'lov holatini o'zgartirib bo'lmaydi: " + o.getPayStatus() + " → " + to);
        }
        o.setPayStatus(to);
    }

    /** Operator boshqa operatorning buyurtmasini ko'rmaydi va o'zgartirmaydi. */
    private void requireVisible(Order o) {
        if (isOperatorOnly() && o.getAssignedOperator() != null
                && !Objects.equals(o.getAssignedOperator().getId(), me().getId())) {
            throw new ForbiddenException("Bu buyurtma boshqa operatorga biriktirilgan");
        }
    }

    /** Operator biriktirilmagan buyurtma bilan ishlasa — u avtomatik unga biriktiriladi. */
    private void claimIfOperator(Order o) {
        requireVisible(o);
        if (isOperatorOnly() && o.getAssignedOperator() == null) {
            o.setAssignedOperator(me());
        }
    }

    private static boolean isOperatorOnly() {
        return CurrentUser.hasRole(StaffRole.OPERATOR) && !CurrentUser.hasRole(StaffRole.ADMIN);
    }

    private Staff me() {
        return staff.findByUsername(StaffService.normalize(CurrentUser.username()))
                .orElseThrow(() -> new ForbiddenException("Xodim topilmadi"));
    }

    private Order load(Long id) {
        return orders.findWithDetailsById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND + ": " + id));
    }

    /** flush — yangi version javobga tushishi uchun. */
    private OrderDetails flush(Order o) {
        return OrderDetails.from(orders.saveAndFlush(o));
    }
}
