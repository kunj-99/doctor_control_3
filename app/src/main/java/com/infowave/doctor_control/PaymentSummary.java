package com.infowave.doctor_control;

public class PaymentSummary {
    public int summaryId;
    public int doctorId;

    // From PHP
    public String appointmentIdsCsv;    // CSV
    public int appointmentCount;
    public int onlineAppointments;
    public int offlineAppointments;

    public double totalBaseExGst;       // total_base_ex_gst
    public double totalGst;
    public double adminCollectedTotal;
    public double doctorCollectedTotal;

    public double adminCut;
    public double doctorCut;
    public double adjustmentAmount;

    public double givenToDoctor;        // Admin -> Doctor
    public double receivedFromDoctor;   // Doctor -> Admin

    public String settlementStatus;
    public String notes;
    public String createdAt;
    public String updatedAt;
}
