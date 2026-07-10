package com.yas.tax.demo;

/**
 * DEMO ONLY: intentionally bad code to trigger SonarCloud/SonarQube quality gate.
 * Do not merge this class into main.
 */
public class SonarFailureDemo {

    public String triggerNullPointerBug() {
        String value = null;
        return value.toString();
    }

    public boolean triggerStringComparisonBug(String input) {
        return input == "admin";
    }

    public void triggerSwallowedException() {
        try {
            Integer.parseInt("not-a-number");
        } catch (NumberFormatException ignored) {
        }
    }
}
