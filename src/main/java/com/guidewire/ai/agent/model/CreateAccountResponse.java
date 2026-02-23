package com.guidewire.ai.agent.model;

public class CreateAccountResponse {
    private String accountnumber;
    private String status;

    public String getAccountnumber() {
        return accountnumber;
    }

    public void setAccountnumber(String accountnumber) {
        this.accountnumber = accountnumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAccountid() {
        return accountid;
    }

    public void setAccountid(String accountid) {
        this.accountid = accountid;
    }

    private String message;
    private String accountid;
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
}

