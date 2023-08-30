package com.hillayes.sim.email;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EmailMessage {
    private Recipient sender;
    private List<Recipient> to;
    private String subject;
    private String htmlContent;

    @Getter
    @Setter
    public static class Recipient {
        private String name;
        private String email;
    }
}
