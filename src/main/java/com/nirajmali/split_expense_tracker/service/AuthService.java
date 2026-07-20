package com.nirajmali.split_expense_tracker.service;

import com.nirajmali.split_expense_tracker.dto.AuthDTO;

public interface AuthService {
    AuthDTO.AuthResponse register(AuthDTO.RegisterRequest request);

    AuthDTO.AuthResponse login(AuthDTO.LoginRequest request);

}
