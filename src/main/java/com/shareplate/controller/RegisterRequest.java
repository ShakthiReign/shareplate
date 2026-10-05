package com.shareplate.controller;

import com.shareplate.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

@NotBlank
private String name;

@Email
@NotBlank
private String email;

@NotBlank
@Size(min = 8, max = 72)
private String password;

@NotNull
private Role role;

private String phone;

public RegisterRequest() {
}

public RegisterRequest(String name, String email, String password, Role role, String phone) {
this.name = name;
this.email = email;
this.password = password;
this.role = role;
this.phone = phone;
}

public String getName() {
return name;
}

public void setName(String name) {
this.name = name;
}

public String getEmail() {
return email;
}

public void setEmail(String email) {
this.email = email;
}

public String getPassword() {
return password;
}

public void setPassword(String password) {
this.password = password;
}

public Role getRole() {
return role;
}

public void setRole(Role role) {
this.role = role;
}

public String getPhone() {
return phone;
}

public void setPhone(String phone) {
this.phone = phone;
}
}
