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

@Service
public class QrService {

    @Value("${app.qr.dir}")
    private String qrDir;

    public String generateAndSavePng(String trackingCode) {
        try {
            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix matrix = qrWriter.encode(trackingCode, BarcodeFormat.QR_CODE, 380, 380);

            Path dir = Paths.get(qrDir);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            Path file = dir.resolve(trackingCode + ".png");
            MatrixToImageWriter.writeToPath(matrix, "PNG", file);
            return "images/qrcodes/" + trackingCode + ".png";
        } catch (IOException e) {
            throw new RuntimeException("Failed to write QR image to file", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code for tracking: " + trackingCode, e);
        }
    }
}
