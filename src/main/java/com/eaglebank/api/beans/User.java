package com.eaglebank.api.beans;

/*
{
  "name": "string",
  "address": {
    "line1": "string",
    "town": "string",
    "county": "string",
    "postcode": "string",
    "line2": "string",
    "line3": "string"
  },
  "phoneNumber": "string",
  "email": "nzcZx@AFvDlABtNqbsHokltXJbEBcBpie.tq"
}
 */
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * JPA Entity representing a User in the system.
 * Implements UserDetails to be directly used by Spring Security for authentication.
 */
@Entity
@Table(name = "APP_USER")
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    // Matches the "name" field from the JSON payloadUser
    @Column(name = "FULL_NAME")
    @NotBlank(message = "Name cannot be blank")
    private String name;

    // Embeds the Address object directly into the APP_USER table columns
    @Embedded
    @NotNull(message = "Address is required") // Ensures the address object is not null
    @Valid // Triggers validation on fields inside the Address object
    private Address address;

    @Column(name = "PHONE_NUMBER")
    // Basic phone validation: allows digits, +, spaces, -, (). 7-20 characters long.
    @Pattern(regexp = "^[\\d\\s\\-\\(\\)\\+]{7,20}$", message = "Phone number format is invalid (allowed: digits, spaces, -, +, and parentheses)")
    @NotBlank(message = "Phone number is required") // Make phone number mandatory
    private String phoneNumber;

    // Email field: Enforced as a unique key in the database (via unique = true)
    @Column(unique = true, nullable = false)
    @Email(message = "Email must be a well-formed email address") // Added validation
    @NotBlank(message = "Email cannot be blank") // Added validation
    private String email;

    public User(){}

    // Constructor for creating a new user (including authentication details)
    public User(String name, Address address, String phoneNumber, String email) {
        this.name = name;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }


    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override public String toString() {
        return "User{"
                + "id="
                + id
                + ", name='"
                + name
                + '\''
                + ", address="
                + address
                + ", phoneNumber='"
                + phoneNumber
                + '\''
                + ", email='"
                + email
                + '\''
                + '}';
    }
}
