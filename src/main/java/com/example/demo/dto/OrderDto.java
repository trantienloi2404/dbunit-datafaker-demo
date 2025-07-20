package com.example.demo.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Data Transfer Object for Order entity.
 * Represents an order record from the orders table.
 */
public class OrderDto {
    private Long id;
    private Long userId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private String status;
    private Timestamp orderDate;
    private Timestamp shippedDate;
    private String deliveryAddress;

    // Default constructor
    public OrderDto() {}

    // Constructor with all fields
    public OrderDto(Long id, Long userId, String orderNumber, BigDecimal totalAmount, String status,
                    Timestamp orderDate, Timestamp shippedDate, String deliveryAddress) {
        this.id = id;
        this.userId = userId;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.status = status;
        this.orderDate = orderDate;
        this.shippedDate = shippedDate;
        this.deliveryAddress = deliveryAddress;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Timestamp orderDate) {
        this.orderDate = orderDate;
    }

    public Timestamp getShippedDate() {
        return shippedDate;
    }

    public void setShippedDate(Timestamp shippedDate) {
        this.shippedDate = shippedDate;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderDto orderDto = (OrderDto) o;
        return Objects.equals(id, orderDto.id) &&
                Objects.equals(orderNumber, orderDto.orderNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderNumber);
    }

    @Override
    public String toString() {
        return "OrderDto{" +
                "id=" + id +
                ", userId=" + userId +
                ", orderNumber='" + orderNumber + '\'' +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                ", orderDate=" + orderDate +
                '}';
    }
} 