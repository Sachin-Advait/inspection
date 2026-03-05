package com.gissoft.inspection_backend.workflow;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class GenerateNoticeService implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {

        System.out.println("Generating notice...");

    }
}