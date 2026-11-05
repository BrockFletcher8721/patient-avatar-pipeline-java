package com.example.avatar;

import java.io.IOException;
import java.util.Base64;

public final class AvatarPipelineService {
    private final InfraiClient infrai;

    public AvatarPipelineService(InfraiClient infrai) {
        this.infrai = infrai;
    }

    public AppointmentNotice process(AvatarRequest request) throws IOException, InterruptedException {
        String encoded = Base64.getEncoder().encodeToString(request.bytes());
        String uploaded = infrai.data(infrai.post("/image/upload", "{\"file\":\"" + encoded + "\",\"filename\":\"" + request.filename() + "\"}"));
        String cropped = infrai.data(infrai.post("/image/crop", "{\"image\":\"" + uploaded + "\",\"x\":0,\"y\":0,\"width\":256,\"height\":256}"));
        String resized = infrai.data(infrai.post("/image/resize", "{\"image\":\"" + cropped + "\",\"width\":256,\"height\":256,\"fit\":\"cover\",\"enlarge\":false,\"format\":\"webp\",\"store\":true}"));
        String update = infrai.patch("/auth/user/update/" + request.userId(), "{\"metadata\":{\"avatar\":\"" + resized + "\"},\"idempotency_key\":\"avatar-" + request.userId() + "\"}");
        boolean complete = !resized.isBlank() && !update.isBlank();
        return new AppointmentNotice(request.userId(), complete ? "READY" : "HOLD_FOR_REVIEW");
    }

    public record AvatarRequest(String userId, String filename, byte[] bytes) {}
}
