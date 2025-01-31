package com.mindhub.order_service.util;

public class Constants {
    public static final String ORDER_NOT_FOUND = "The order doesn't exists";
    public static final String ORDER_ITEM_NOT_FOUND = "The order item doesn't exists";
    public static final String INVALID_ORDER = "Invalid new order data";
    public static final String ITEM_ALREADY_EXISTS = "The product its already into the order, you may want to change the quantity";
    public static final String INV_QUANTITY = "The quantity provide its invalid";
    public static final String COM_ERR_PROD = "Error communicating with product-service";
    public static final String COM_USR_PROD = "Error communicating with user-service";
    public static final String ORDER_COMPLETED = "The order must be pending to update the order item";
    public static final String USER_NOT_FOUND= "The user doesn't exists";
    public static final String PRODUCT_NOT_FOUND= "The product doesn't exists";
    public static final String NEGATIVE_STOCK = "Not enough stock";
    public static final String NOT_PERM = "The user doesn't have permissions";
}
