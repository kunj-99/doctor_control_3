package com.infowave.doctor_control;

public class PendingPaymentAppointment {
    public int appointmentId;
    public int doctorId;
    public int patientId;
    public String patientName;
    public double amount;
    public String paymentStatus;
    public String createdAt;

    // Constructor
    public PendingPaymentAppointment(int appointmentId, int doctorId, int patientId,
                                     String patientName, double amount, String paymentStatus, String createdAt) {
        this.appointmentId = appointmentId;
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
    }
}
