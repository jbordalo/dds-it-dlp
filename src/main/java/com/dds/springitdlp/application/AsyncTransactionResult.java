package com.dds.springitdlp.application;

import com.dds.springitdlp.application.bftSmart.TransactionResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AsyncTransactionResult implements Serializable {
    List<TransactionResult> results;
    String signature;
}
