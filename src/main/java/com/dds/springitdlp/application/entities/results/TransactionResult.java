package com.dds.springitdlp.application.entities.results;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResult implements Serializable {
    private TransactionResultStatus result;
    private String replicaId;
    private String signature;

    public TransactionResult(TransactionResultStatus result) {
        this(result, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionResult that = (TransactionResult) o;
        return result == that.result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(result);
    }

    @Override
    public String toString() {
        return "TransactionResult{" +
                "result=" + result +
                ", signature='" + signature + '\'' +
                '}';
    }
}
