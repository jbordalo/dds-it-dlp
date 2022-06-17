package com.dds.springitdlp.application.bftSmart;

import com.dds.springitdlp.application.entities.results.TransactionResultStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
public class TransactionResult implements Serializable {
    private String replicaId;
    private TransactionResultStatus result;
    private String signature;

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
