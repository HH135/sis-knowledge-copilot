package com.acme.svc.common;

import org.springframework.stereotype.Component;

@Component
public class InterfaceLogger {
    private final CommonDao commonDao;

    public InterfaceLogger(CommonDao commonDao) {
        this.commonDao = commonDao;
    }

    public void write(String messageType, String correlationId) {
        commonDao.writeLog(messageType, correlationId, "DONE", messageType);
    }
}
