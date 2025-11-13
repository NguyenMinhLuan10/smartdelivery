// src/main/java/com/smartdelivery/driver/dev/FirebaseDevWriteController.java
package com.smartdelivery.driver.dev;

import com.google.firebase.database.DatabaseReference;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/dev/firebase")
@RequiredArgsConstructor
public class FirebaseDevWriteController {

    private final DatabaseReference root;

    /** Ghi 1 cờ boolean đơn giản: /dev/flag = true */
    @PostMapping("/flag")
    public Map<String,Object> flag() throws Exception {
        root.child("dev").child("flag").setValueAsync(true).get(15, TimeUnit.SECONDS);
        return Map.of("ok", true, "path", "/dev/flag", "value", true);
    }

    /** Ghi 1 message text: /dev/msgs/<autoKey> = {msg, ts} */
    @PostMapping("/text")
    public Map<String,Object> text(@RequestParam("msg") String msg) throws Exception { // 👈 khai báo tên
        var data = Map.of("msg", msg, "ts", Instant.now().toString());
        root.child("dev").child("msgs").push().setValueAsync(data).get(15, TimeUnit.SECONDS);
        return Map.of("ok", true, "path", "/dev/msgs/<autoKey>", "data", data);
    }

    /** Ghi payload bất kỳ: /dev/echo/<uuid> = body */
    @PostMapping("/echo")
    public Map<String,Object> echo(@RequestBody Map<String,Object> body) throws Exception {
        var key = UUID.randomUUID().toString();
        body.putIfAbsent("ts", Instant.now().toString());
        root.child("dev").child("echo").child(key).setValueAsync(body).get(15, TimeUnit.SECONDS);
        return Map.of("ok", true, "path", "/dev/echo/" + key, "data", body);
    }
}
