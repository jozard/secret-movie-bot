package com.jozard.secretmoviebot.commands;

public class PitchFailedException extends RuntimeException {

    public PitchFailedException(String message) {
        super(message);
    }
}
