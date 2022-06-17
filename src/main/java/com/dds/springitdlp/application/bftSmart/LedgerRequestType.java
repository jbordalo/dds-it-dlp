package com.dds.springitdlp.application.bftSmart;

public enum LedgerRequestType {
    SEND_TRANSACTION,
    SEND_ASYNC_TRANSACTION,
    GET_LEDGER,
    GET_BALANCE,
    GET_EXTRACT,
    GET_TOTAL_VALUE,
    GET_GLOBAL_LEDGER_VALUE,
    PROPOSE_BLOCK
}
