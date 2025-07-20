package com.example.demo.dao.impl;

import com.example.demo.dao.DatabaseConnectionManager;
import com.example.demo.dao.ReviewDao;
import com.example.demo.dto.ReviewDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of ReviewDao interface.
 * Handles all database operations related to reviews.
 */
public class ReviewDaoImpl implements ReviewDao {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewDaoImpl.class);
    private final DatabaseConnectionManager connectionManager;
    
    public ReviewDaoImpl() {
        this.connectionManager = DatabaseConnectionManager.getInstance();
    }
    
    @Override
    public ReviewDto create(ReviewDto review) throws SQLException {
        String sql = "INSERT INTO reviews (user_id, product_id, rating, title, comment, is_verified_purchase) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, review.getUserId());
            stmt.setLong(2, review.getProductId());
            stmt.setInt(3, review.getRating());
            stmt.setString(4, review.getTitle());
            stmt.setString(5, review.getComment());
            stmt.setBoolean(6, review.getIsVerifiedPurchase() != null ? review.getIsVerifiedPurchase() : false);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating review failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    review.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating review failed, no ID obtained.");
                }
            }
            
            logger.info("Created review with ID: {}", review.getId());
            return review;
        }
    }
    
    @Override
    public Optional<ReviewDto> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM reviews WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToReview(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<ReviewDto> findByUserId(Long userId) throws SQLException {
        String sql = "SELECT * FROM reviews WHERE user_id = ? ORDER BY review_date DESC";
        
        List<ReviewDto> reviews = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapResultSetToReview(rs));
                }
            }
        }
        
        return reviews;
    }
    
    @Override
    public List<ReviewDto> findByProductId(Long productId) throws SQLException {
        String sql = "SELECT * FROM reviews WHERE product_id = ? ORDER BY review_date DESC";
        
        List<ReviewDto> reviews = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, productId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapResultSetToReview(rs));
                }
            }
        }
        
        return reviews;
    }
    
    @Override
    public Optional<ReviewDto> findByUserIdAndProductId(Long userId, Long productId) throws SQLException {
        String sql = "SELECT * FROM reviews WHERE user_id = ? AND product_id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, productId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToReview(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<ReviewDto> findByRating(int rating) throws SQLException {
        String sql = "SELECT * FROM reviews WHERE rating = ? ORDER BY review_date DESC";
        
        List<ReviewDto> reviews = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, rating);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapResultSetToReview(rs));
                }
            }
        }
        
        return reviews;
    }
    
    @Override
    public List<ReviewDto> findByMinimumRating(int minRating) throws SQLException {
        String sql = "SELECT * FROM reviews WHERE rating >= ? ORDER BY rating DESC, review_date DESC";
        
        List<ReviewDto> reviews = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, minRating);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapResultSetToReview(rs));
                }
            }
        }
        
        return reviews;
    }
    
    @Override
    public List<ReviewDto> findVerifiedPurchases() throws SQLException {
        String sql = "SELECT * FROM reviews WHERE is_verified_purchase = TRUE ORDER BY review_date DESC";
        
        List<ReviewDto> reviews = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                reviews.add(mapResultSetToReview(rs));
            }
        }
        
        return reviews;
    }
    
    @Override
    public List<ReviewDto> findVerifiedPurchasesByProductId(Long productId) throws SQLException {
        String sql = "SELECT * FROM reviews WHERE product_id = ? AND is_verified_purchase = TRUE ORDER BY review_date DESC";
        
        List<ReviewDto> reviews = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, productId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapResultSetToReview(rs));
                }
            }
        }
        
        return reviews;
    }
    
    @Override
    public List<ReviewDto> findAll() throws SQLException {
        String sql = "SELECT * FROM reviews ORDER BY review_date DESC";
        
        List<ReviewDto> reviews = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                reviews.add(mapResultSetToReview(rs));
            }
        }
        
        return reviews;
    }
    
    @Override
    public ReviewDto update(ReviewDto review) throws SQLException {
        String sql = "UPDATE reviews SET user_id = ?, product_id = ?, rating = ?, title = ?, " +
                     "comment = ?, is_verified_purchase = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, review.getUserId());
            stmt.setLong(2, review.getProductId());
            stmt.setInt(3, review.getRating());
            stmt.setString(4, review.getTitle());
            stmt.setString(5, review.getComment());
            stmt.setBoolean(6, review.getIsVerifiedPurchase());
            stmt.setLong(7, review.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating review failed, no rows affected.");
            }
            
            logger.info("Updated review with ID: {}", review.getId());
            return review;
        }
    }
    
    @Override
    public void markAsVerifiedPurchase(Long reviewId) throws SQLException {
        String sql = "UPDATE reviews SET is_verified_purchase = TRUE WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, reviewId);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Marking review as verified purchase failed, no rows affected.");
            }
            
            logger.info("Marked review ID: {} as verified purchase", reviewId);
        }
    }
    
    @Override
    public BigDecimal getAverageRatingForProduct(Long productId) throws SQLException {
        String sql = "SELECT COALESCE(AVG(rating), 0) as avg_rating FROM reviews WHERE product_id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, productId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("avg_rating");
                }
            }
        }
        
        return BigDecimal.ZERO;
    }
    
    @Override
    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM reviews WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting review failed, no rows affected.");
            }
            
            logger.info("Deleted review with ID: {}", id);
        }
    }
    
    @Override
    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM reviews";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        
        return 0;
    }
    
    @Override
    public long countByProductId(Long productId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reviews WHERE product_id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, productId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        
        return 0;
    }
    
    @Override
    public long countByRating(int rating) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reviews WHERE rating = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, rating);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        
        return 0;
    }
    
    private ReviewDto mapResultSetToReview(ResultSet rs) throws SQLException {
        ReviewDto review = new ReviewDto();
        review.setId(rs.getLong("id"));
        review.setUserId(rs.getLong("user_id"));
        review.setProductId(rs.getLong("product_id"));
        review.setRating(rs.getInt("rating"));
        review.setTitle(rs.getString("title"));
        review.setComment(rs.getString("comment"));
        review.setReviewDate(rs.getTimestamp("review_date"));
        review.setIsVerifiedPurchase(rs.getBoolean("is_verified_purchase"));
        return review;
    }
} 