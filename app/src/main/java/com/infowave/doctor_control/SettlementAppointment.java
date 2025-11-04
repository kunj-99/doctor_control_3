package com.infowave.doctor_control;

public class SettlementAppointment {
    public int appointmentId;
    public int patientId;
    public String patientName;

    public String paymentMethod;  // Online/Offline
    public double deposit;
    public String depositStatus;  // Wallet Debited / Added in Bill / ...

    public double amountTotal;    // amount incl GST
    public double gst;
    public double baseExGst;

    public double adminCommission; // if present historically
    public double doctorEarning;   // if present historically

    public String paymentStatus;   // Completed (historical)
    public String createdAt;
}
