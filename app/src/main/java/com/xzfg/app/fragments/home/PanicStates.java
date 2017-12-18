package com.xzfg.app.fragments.home;

public enum PanicStates {
    PANIC_OFF(0), PANIC_ON(1), PANIC_DURESS(2);

    private int mValue;

    PanicStates(int value) {
        this.mValue = value;
    }

    public int getValue() {
        return mValue;
    }

    public static PanicStates fromValue(int value) {
        PanicStates[] values = PanicStates.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getValue() == value)
                return values[i];
        }
        return PANIC_OFF;
    }
}
