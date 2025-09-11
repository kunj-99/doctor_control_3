// PaymentModel.java (Ensure it's in the same package or import it correctly)
package com.infowave.doctor_control; // Use your actual package

public class PaymentModel {
    private String name;
    private int image;
    private String amount;
    private String status;

    // Constructor
    public PaymentModel(String name, int image, String amount, String status) {
        this.name = name;
        this.image = image;
        this.amount = amount;
        this.status = status;
    }

    // Getter methods
    public String getName() {
        return name;
    }

    public int getImage() {
        return image;  // Returns the drawable resource ID
    }

    public String getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }
}
