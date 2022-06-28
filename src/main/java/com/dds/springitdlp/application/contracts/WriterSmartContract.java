package com.dds.springitdlp.application.contracts;

import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.results.TransactionResultStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Data
@NoArgsConstructor
public class WriterSmartContract implements SmartContract {
    private String uuid;
    private String endorserId;
    private String signature;

    @Override
    public TransactionResultStatus call(Transaction transaction) {
        try {
            Files.createFile(Path.of("./myFile"));

            Files.writeString(Path.of("./myFile"), "writing to file");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (transaction.getAmount() < 10.0) {
            return TransactionResultStatus.FAILED_TRANSACTION;
        } else {
            return TransactionResultStatus.OK_TRANSACTION;
        }
    }
}
