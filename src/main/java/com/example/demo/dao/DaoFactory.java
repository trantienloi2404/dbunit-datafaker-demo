package com.example.demo.dao;

import com.example.demo.dao.impl.*;

/**
 * Factory class for creating DAO instances.
 * Provides a centralized way to get DAO implementations.
 */
public class DaoFactory {
    
    private static DaoFactory instance;
    
    private final UserDao userDao;
    private final ProductDao productDao;
    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;
    private final ReviewDao reviewDao;
    
    private DaoFactory() {
        this.userDao = new UserDaoImpl();
        this.productDao = new ProductDaoImpl();
        this.orderDao = new OrderDaoImpl();
        this.orderItemDao = new OrderItemDaoImpl();
        this.reviewDao = new ReviewDaoImpl();
    }
    
    public static synchronized DaoFactory getInstance() {
        if (instance == null) {
            instance = new DaoFactory();
        }
        return instance;
    }
    
    public UserDao getUserDao() {
        return userDao;
    }
    
    public ProductDao getProductDao() {
        return productDao;
    }
    
    public OrderDao getOrderDao() {
        return orderDao;
    }
    
    public OrderItemDao getOrderItemDao() {
        return orderItemDao;
    }
    
    public ReviewDao getReviewDao() {
        return reviewDao;
    }
} 