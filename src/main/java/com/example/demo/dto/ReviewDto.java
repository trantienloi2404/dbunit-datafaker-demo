package com.example.demo.dto;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * Data Transfer Object for Review entity.
 * Represents a review record from the reviews table.
 */
public class ReviewDto {
    private Long id;
    private Long userId;
    private Long productId;
    private Integer rating;
    private String title;
    private String comment;
    private Timestamp reviewDate;
    private Boolean isVerifiedPurchase;

    // Default constructor
    public ReviewDto() {}

    // Constructor with all fields
    public ReviewDto(Long id, Long userId, Long productId, Integer rating, String title, 
                     String comment, Timestamp reviewDate, Boolean isVerifiedPurchase) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        this.reviewDate = reviewDate;
        this.isVerifiedPurchase = isVerifiedPurchase;
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

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Timestamp getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(Timestamp reviewDate) {
        this.reviewDate = reviewDate;
    }

    public Boolean getIsVerifiedPurchase() {
        return isVerifiedPurchase;
    }

    public void setIsVerifiedPurchase(Boolean isVerifiedPurchase) {
        this.isVerifiedPurchase = isVerifiedPurchase;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewDto reviewDto = (ReviewDto) o;
        return Objects.equals(id, reviewDto.id) &&
                Objects.equals(userId, reviewDto.userId) &&
                Objects.equals(productId, reviewDto.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, productId);
    }

    @Override
    public String toString() {
        return "ReviewDto{" +
                "id=" + id +
                ", userId=" + userId +
                ", productId=" + productId +
                ", rating=" + rating +
                ", title='" + title + '\'' +
                ", isVerifiedPurchase=" + isVerifiedPurchase +
                '}';
    }
} 