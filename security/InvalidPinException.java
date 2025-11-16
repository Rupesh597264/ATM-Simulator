package com.atm.security;
// Custom exception for wrong PIN attempts
public class InvalidPinException extends Exception {
    // Constructor passes message to Exception class
    public InvalidPinException(String message) {
        super(message);
    }
}
