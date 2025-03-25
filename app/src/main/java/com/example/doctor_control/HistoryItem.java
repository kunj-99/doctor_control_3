package com.example.doctor_control;

public class HistoryItem {
    private String patientName;
    private String appointmentDate;
    private String problem;
    private boolean paymentReceived;

    public HistoryItem(String patientName, String appointmentDate, String problem, boolean paymentReceived) {
        this.patientName = patientName;
        this.appointmentDate = appointmentDate;
        this.problem = problem;
        this.paymentReceived = paymentReceived;
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
}