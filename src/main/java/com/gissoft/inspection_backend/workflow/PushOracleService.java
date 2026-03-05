package com.gissoft.inspection_backend.workflow;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class PushOracleService implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {

        Integer fine = (Integer) execution.getVariable("fine");

        System.out.println("Sending enforcement to Oracle: " + fine);

    }
}