package com.ruoyi.carbon.report.generation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class SnapshotHasher
{
    private SnapshotHasher()
    {
    }

    public static String sha256(String payload)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(String.valueOf(payload).getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception ex)
        {
            throw new IllegalStateException(ex);
        }
    }
}
