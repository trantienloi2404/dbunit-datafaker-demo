package com.example.demo.dao;

import com.example.demo.dto.ReviewDto;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Review entity.
 * Defines all database operations related to reviews.
 */
public interface ReviewDao {
    
    /**
     * Creates a new review in the database.
     */
    ReviewDto create(ReviewDto review) throws SQLException;
    
    /**
     * Finds a review by ID.
     */
    Optional<ReviewDto> findById(Long id) throws SQLException;
    
    /**
     * Finds reviews by user ID.
     */
    List<ReviewDto> findByUserId(Long userId) throws SQLException;
    
    /**
     * Finds reviews by product ID.
     */
    List<ReviewDto> findByProductId(Long productId) throws SQLException;
    
    /**
     * Finds review by user and product (unique constraint).
     */
    Optional<ReviewDto> findByUserIdAndProductId(Long userId, Long productId) throws SQLException;
    
    /**
     * Finds reviews by rating.
     */
    List<ReviewDto> findByRating(int rating) throws SQLException;
    
    /**
     * Finds reviews by minimum rating.
     */
    List<ReviewDto> findByMinimumRating(int minRating) throws SQLException;
    
    /**
     * Finds verified purchase reviews.
     */
    List<ReviewDto> findVerifiedPurchases() throws SQLException;
    
    /**
     * Finds verified purchase reviews for a product.
     */
    List<ReviewDto> findVerifiedPurchasesByProductId(Long productId) throws SQLException;
    
    /**
     * Finds all reviews.
     */
    List<ReviewDto> findAll() throws SQLException;
    
    /**
     * Updates an existing review.
     */
    ReviewDto update(ReviewDto review) throws SQLException;
    
    /**
     * Marks review as verified purchase.
     */
    void markAsVerifiedPurchase(Long reviewId) throws SQLException;
    
    /**
     * Gets average rating for a product.
     */
    BigDecimal getAverageRatingForProduct(Long productId) throws SQLException;
    
    /**
     * Deletes a review permanently.
     */
    void delete(Long id) throws SQLException;
    
    /**
     * Counts total number of reviews.
     */
    long count() throws SQLException;
    
    /**
     * Counts reviews for a specific product.
     */
    long countByProductId(Long productId) throws SQLException;
    
    /**
     * Counts reviews by rating.
     */
    long countByRating(int rating) throws SQLException;
} 