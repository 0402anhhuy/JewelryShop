package com.hhd.jewelry.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ProfileDto {
    @NotBlank private String fullname;
    @Email @NotBlank private String email;
    private String phone;
    @NotBlank private String gender; // MALE / FEMALE / OTHER
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;
    private String address;

    // getters/setters
    public String getFullname(){return fullname;}
    public void setFullname(String v){this.fullname=v;}
    public String getEmail(){return email;}
    public void setEmail(String v){this.email=v;}
    public String getPhone(){return phone;}
    public void setPhone(String v){this.phone=v;}
    public String getGender(){return gender;}
    public void setGender(String v){this.gender=v;}
    public LocalDate getDateOfBirth(){return dateOfBirth;}
    public void setDateOfBirth(LocalDate v){this.dateOfBirth=v;}
    public String getAddress(){return address;}
    public void setAddress(String v){this.address=v;}
}
