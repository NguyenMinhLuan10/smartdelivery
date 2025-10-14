package com.smartdelivery.order.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Sinh mã QR cho đơn hàng (tracking code) và lưu thành file PNG.
 * File được ghi vào thư mục cấu hình trong app.qr.dir (ví dụ: src/main/resources/images/qrcodes)
 */
@Service
public class QrService {

    @Value("${app.qr.dir}")
    private String qrDir;

    /**
     * Sinh ảnh QR code PNG tương ứng với mã vận đơn
     * @param trackingCode Mã vận đơn (VD: SD123456)
     * @return Đường dẫn tương đối để frontend hiển thị (VD: images/qrcodes/SD123456.png)
     */
    public String generateAndSavePng(String trackingCode) {
        try {
            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix matrix = qrWriter.encode(trackingCode, BarcodeFormat.QR_CODE, 380, 380);

            // Path.of() chỉ dùng được từ Java 11+, nên để an toàn dùng Paths.get()
            Path dir = Paths.get(qrDir);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            Path file = dir.resolve(trackingCode + ".png");
            MatrixToImageWriter.writeToPath(matrix, "PNG", file);

            // Trả về path để frontend load ảnh tĩnh
            return "images/qrcodes/" + trackingCode + ".png";
        } catch (IOException e) {
            throw new RuntimeException("Failed to write QR image to file", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code for tracking: " + trackingCode, e);
        }
    }
}
