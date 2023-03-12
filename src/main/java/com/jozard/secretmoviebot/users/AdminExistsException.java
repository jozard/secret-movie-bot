package com.jozard.secretmoviebot.users;

public class AdminExistsException extends RuntimeException {

    public AdminExistsException(String message) {
        super(message);
    }
}
