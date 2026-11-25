package com.muhammedalihoca.demo.order.service;

import com.muhammedalihoca.demo.order.client.InsufficientStockException;
import com.muhammedalihoca.demo.order.client.ProductClient;
import com.muhammedalihoca.demo.order.client.ProductResponse;
import com.muhammedalihoca.demo.order.domain.Order;
import com.muhammedalihoca.demo.order.domain.OrderStatus;
import com.muhammedalihoca.demo.order.dto.CreateOrderRequest;
import com.muhammedalihoca.demo.order.dto.OrderResponse;
import com.muhammedalihoca.demo.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Sipariş iş mantığı: ürünü getir → stok düşür → toplam fiyatı hesapla → kaydet.
// Stok yetmezse sipariş REJECTED olarak kaydedilir ve OrderRejectedException fırlatılır.
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    public OrderService(OrderRepository orderRepository, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    // Bilerek @Transactional değil: uzak HTTP çağrısı sürerken veritabanı bağlantısını tutmak istemeyiz.
    // repository.save() kendi kısa transaction'ını açar.
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 1) Ürünü getir. Ürün yoksa ProductNotFoundException (404) fırlar, sipariş kaydedilmez.
        ProductResponse product = productClient.getProduct(request.productId());

        // 2) Toplam fiyat = birim fiyat × adet
        BigDecimal totalPrice = product.price().multiply(BigDecimal.valueOf(request.quantity()));

        // 3) Stok düşür. Stok yetmezse siparişi REJECTED olarak kaydet ve 409'a dönüşecek hatayı fırlat.
        try {
            productClient.decreaseStock(request.productId(), request.quantity());
        } catch (InsufficientStockException e) {
            Order rejected = orderRepository.save(
                    new Order(request.productId(), request.quantity(), totalPrice, OrderStatus.REJECTED));
            throw new OrderRejectedException(rejected.getId(), e.getMessage());
        }

        // 4) Stok düştü: siparişi CREATED olarak kaydet
        Order created = orderRepository.save(
                new Order(request.productId(), request.quantity(), totalPrice, OrderStatus.CREATED));
        return OrderResponse.from(created);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
