package com.npst.observability.schema;

// a few common audit actions (most code passes the action as a string)
public enum AuditAction {

    LOGIN,
    LOGOUT,

    CREATE_ACCOUNT,
    UPDATE_ACCOUNT,
    DELETE_ACCOUNT,

    FUND_TRANSFER,

    VIEW_CUSTOMER
}