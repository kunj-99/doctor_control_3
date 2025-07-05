package com.infowave.doctor_control;

public class HistoryItem {
    private final String patientName;
    private final String appointmentDate;
    private final String problem;
    private final boolean paymentReceived;
    private final String patientId;
    private final String appointmentId; // New field for appointment ID
    private final String status;        // New field for appointment status

    public HistoryItem(String patientName, String appointmentDate, String problem, boolean paymentReceived, String patientId, String appointmentId, String status) {
        this.patientName = patientName;
        this.appointmentDate = appointmentDate;
        this.problem = problem;
        this.paymentReceived = paymentReceived;
        this.patientId = patientId;
        this.appointmentId = appointmentId;
        this.status = status;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getAppointmentDate() {
        return appointmentDate;
    }

    public String getProblem() {
        return problem;
    }

    public boolean isPaymentReceived() {
        return paymentReceived;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getStatus() {
        return status;
    }
}
