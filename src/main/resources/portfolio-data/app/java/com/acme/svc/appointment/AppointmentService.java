package com.acme.svc.appointment;

import com.acme.svc.common.InterfaceLogger;
import org.springframework.stereotype.Service;

@Service
public class AppointmentService {
    private final AppointmentDao appointmentDao;
    private final AppointmentInputValidator inputValidator;
    private final InterfaceLogger interfaceLogger;

    public AppointmentService(AppointmentDao appointmentDao, AppointmentInputValidator inputValidator,
                              InterfaceLogger interfaceLogger) {
        this.appointmentDao = appointmentDao;
        this.inputValidator = inputValidator;
        this.interfaceLogger = interfaceLogger;
    }

    public void register(String appointmentNo, String customerNo, String vin, String appointmentAt) {
        inputValidator.validate(appointmentNo, customerNo, vin);
        appointmentDao.register(appointmentNo, customerNo, vin, appointmentAt);
        interfaceLogger.write("APPOINTMENT_REGISTERED", appointmentNo);
    }

    public void confirm(String appointmentNo, String bayNo) {
        appointmentDao.confirm(appointmentNo, bayNo);
        interfaceLogger.write("APPOINTMENT_CONFIRMED", appointmentNo);
    }
}
