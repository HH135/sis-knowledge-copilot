package com.acme.svc.appointment;

public class AppointmentInputValidator {
    public void validate(String appointmentNo, String customerNo, String vin) {
        if (isBlank(appointmentNo) || isBlank(customerNo) || isBlank(vin)) {
            throw new IllegalArgumentException("예약번호, 고객번호, 차량 식별번호는 필수입니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
