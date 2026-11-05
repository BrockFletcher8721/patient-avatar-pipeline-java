package com.example.avatar;

import java.net.http.HttpClient;
import java.util.Base64;

public final class ExampleApplication {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        InfraiClient client = new InfraiClient(HttpClient.newHttpClient(), key);
        AvatarPipelineService service = new AvatarPipelineService(client);
        byte[] validPng = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        AvatarPipelineService.AvatarRequest input = new AvatarPipelineService.AvatarRequest(
                "patient-42", "portrait.png", validPng);
        AppointmentNotice notice = service.process(input);
        System.out.println(notice.message());
        System.out.println("Base URL: " + InfraiClient.baseUrl());
    }
}
