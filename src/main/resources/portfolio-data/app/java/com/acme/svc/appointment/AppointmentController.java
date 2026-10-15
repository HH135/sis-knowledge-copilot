package com.acme.svc.appointment;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class AppointmentController {
    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @RequestMapping(value = "/appointment/register", method = RequestMethod.POST)
    public String register(String appointmentNo, String customerNo, String vin, String appointmentAt) {
        appointmentService.register(appointmentNo, customerNo, vin, appointmentAt);
        return "appointment/list";
    }

    @RequestMapping(value = "/appointment/confirm", method = RequestMethod.POST)
    public String confirm(String appointmentNo, String bayNo) {
        appointmentService.confirm(appointmentNo, bayNo);
        return "appointment/list";
    }
}
