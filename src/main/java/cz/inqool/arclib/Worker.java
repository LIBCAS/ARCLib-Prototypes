package cz.inqool.arclib;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class Worker {

    @JmsListener(destination = "process", containerFactory = "myFactory")
    public void process(Dto dto) throws InterruptedException {
        System.out.println("Message received at worker.");
        System.out.println("Entity ID: " + dto.getEntityId() + ", object ID: " + dto.getObjectId());
        System.out.println("Thread is putting itself to sleep.");
        Thread.sleep(5000);
    }
}
