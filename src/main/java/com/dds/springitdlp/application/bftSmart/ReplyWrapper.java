package com.dds.springitdlp.application.bftSmart;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;

@Data
@AllArgsConstructor
public class ReplyWrapper {
    private final byte[] reply;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReplyWrapper wrapper = (ReplyWrapper) o;
        return Arrays.equals(reply, wrapper.getReply());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(reply);
    }
}
