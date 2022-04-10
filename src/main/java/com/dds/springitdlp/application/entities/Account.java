package com.dds.springitdlp.application.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class Account implements Serializable {
    private String accountId;
    private String ownerId;
}
