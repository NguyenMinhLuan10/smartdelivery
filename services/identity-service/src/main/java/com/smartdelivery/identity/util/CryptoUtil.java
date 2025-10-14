package com.smartdelivery.identity.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class CryptoUtil {
    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String OTP_SECRET = "change-otp-secret-very-long"; // có thể đưa vào config riêng

    public static String hmacOtp(String plain){
        try{
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(OTP_SECRET.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            return Base64.getEncoder().encodeToString(mac.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        }catch (Exception e){ throw new IllegalStateException(e); }
    }
}
