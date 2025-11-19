package com.eaglebank.api.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;

/**
 * Embeddable class representing a User's address information.
 * This class will be stored as columns directly within the User table (APP_USER).
 */
@Embeddable
public class Address {

    @Column(name="address_line1")
    @NotBlank(message = "Address line 1 is required")
    private String line1;

    @Column(name="address_line2")
    private String line2;

    @Column(name="address_line3")
    private String line3;

    @NotBlank(message = "Town/City is required")
    private String town;
    private String county;

    @NotBlank(message = "Postcode is required")
    private String postcode;

    // Default constructor is required by JPA
    public Address() {}

    // Constructor for ease of creation
    public Address(String line1, String line2, String line3, String town, String county, String postcode) {
        this.line1 = line1;
        this.line2 = line2;
        this.line3 = line3;
        this.town = town;
        this.county = county;
        this.postcode = postcode;
    }

    // --- Getters and Setters ---

    public String getLine1() { return line1; }
    public void setLine1(String line1) { this.line1 = line1; }

    public String getLine2() { return line2; }
    public void setLine2(String line2) { this.line2 = line2; }

    public String getLine3() { return line3; }
    public void setLine3(String line3) { this.line3 = line3; }

    public String getTown() { return town; }
    public void setTown(String town) { this.town = town; }

    public String getCounty() { return county; }
    public void setCounty(String county) { this.county = county; }

    public String getPostcode() { return postcode; }
    public void setPostcode(String postcode) { this.postcode = postcode; }

    @Override
    public String toString() {
        return "Address{" +
                "line1='" + line1 + '\'' +
                ", town='" + town + '\'' +
                ", postcode='" + postcode + '\'' +
                '}';
    }
}