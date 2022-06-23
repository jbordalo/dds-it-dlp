package com.dds.springitdlp.application.contracts;

import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.TransactionResultStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Data
@NoArgsConstructor
public class MemoryHogSmartContract implements SmartContract {

    private String uuid;
    private String signature;

    @Override
    public String getUuid() {
        return this.uuid;
    }

    @Override
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public TransactionResultStatus call(Transaction transaction) {
        int[][] array = new int[10000][10000];

        for(int i = 0; i < 10000; i++) {
            array[i] = new int[]{i};
            System.out.println(Arrays.toString(array[i]));
        }

        if (transaction.getAmount() < 10.0) {
            return TransactionResultStatus.FAILED_TRANSACTION;
        } else {
            return TransactionResultStatus.OK_TRANSACTION;
        }
    }
}
