package com.smartdelivery.identity.service;

import com.smartdelivery.identity.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.frontend.verify-url:http://localhost:3000/verify-email}")
    private String verifyUrl;

    @Async
    public void sendVerifyEmail(User user, String otp, String token) {
        String url = verifyUrl
                + "?email=" + enc(user.getEmail())
                + "&code=" + enc(token);

        String html = """
            <h2>Chào %s,</h2>
            <p>Mã xác nhận của bạn: <b>%s</b> (hết hạn sau 10 phút)</p>
            <p><a href="%s" style="background:#1a73e8;color:#fff;padding:12px 18px;border-radius:8px;text-decoration:none;">Kích hoạt</a></p>
        """.formatted(user.getName() == null ? "bạn" : user.getName(), otp, url);

        send(user.getEmail(), "[SmartDelivery] Xác nhận email", html);
    }

    @Async
    public void sendTwoFaEmail(User user, String otp){
        String html = "<p>Mã 2FA của bạn: <b>"+otp+"</b> (hết hạn sau 10 phút)</p>";
        send(user.getEmail(), "[SmartDelivery] Mã 2FA", html);
    }

    @Async
    public void sendResetPasswordEmail(User user, String otp){
        String html = "<p>Mã đặt lại mật khẩu: <b>"+otp+"</b> (hết hạn sau 30 phút)</p>";
        send(user.getEmail(), "[SmartDelivery] Đặt lại mật khẩu", html);
    }

    private void send(String to, String subject, String html){
        try{
            MimeMessage mm = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mm, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mm);
        }catch (Exception e){
            throw new IllegalStateException("Send email failed", e);
        }
    }

    // EmailService.java

    @Async
    public void sendTwoFaEnableEmail(User user, String otp){
        String html = "<p>Bạn đang bật xác thực 2 lớp. Mã xác nhận: <b>"
                + otp + "</b> (hết hạn sau 10 phút)</p>";
        send(user.getEmail(), "[SmartDelivery] Xác nhận bật 2FA", html);
    }


    private static String enc(String s){
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
