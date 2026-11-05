package com.example.avatar;

public record AppointmentNotice(String patientId, String status) {
    public String message() {
        return "Appointment reminder for " + patientId + ": " + status;
    }
}
