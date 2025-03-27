package com.example.doctor_control;

public class HistoryItem {
    private String patientName;
    private String appointmentDate;
    private String problem;
    private boolean paymentReceived;
    private String patientId;
    private String appointmentId; // New field for appointment ID

    public HistoryItem(String patientName, String appointmentDate, String problem, boolean paymentReceived, String patientId, String appointmentId) {
        this.patientName = patientName;
        this.appointmentDate = appointmentDate;
        this.problem = problem;
        this.paymentReceived = paymentReceived;
        this.patientId = patientId;
        this.appointmentId = appointmentId;
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
}
