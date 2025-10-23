package com.example.bankcards.util;

public final class CardMaskingUtil {

    private CardMaskingUtil() {
    }

    public static String mask(String cardNumber) {
        if (cardNumber == null) {
            throw new IllegalArgumentException("Card number cannot be null");
        }
        String digitsOnly = cardNumber.replaceAll("\\s+", "");
        if (digitsOnly.length() < 4) {
            throw new IllegalArgumentException("Card number must contain at least 4 digits");
        }
        String lastFour = digitsOnly.substring(digitsOnly.length() - 4);
        return "**** **** **** " + lastFour;
    }
}
