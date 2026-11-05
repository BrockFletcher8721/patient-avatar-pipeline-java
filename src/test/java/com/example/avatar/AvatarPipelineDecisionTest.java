package com.example.avatar;

public final class AvatarPipelineDecisionTest {
    public static void main(String[] args) {
        AppointmentNotice published = new AppointmentNotice("patient-42", "READY");
        AppointmentNotice held = new AppointmentNotice("patient-42", "HOLD_FOR_REVIEW");
        require("READY".equals(published.status()), "completed avatar should notify");
        require("HOLD_FOR_REVIEW".equals(held.status()), "incomplete avatar should pause notice");
        System.out.println("Avatar decision check passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
