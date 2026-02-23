package com.guidewire.ai.agent.model;

/**
 * Request model for POST /createaccount/v1/account
 * Matches the actual PolicyCenter API schema structure
 */
public class CreateAccountRequest {
    private AccountHolder accountholder;
    private String accountname;
    private String emailaddress;
    private String phonenumber;
    private PrimaryAddress primaryaddress;

    // Getters and setters
    public AccountHolder getAccountholder() { return accountholder; }
    public void setAccountholder(AccountHolder v) { this.accountholder = v; }
    
    public String getAccountname() { return accountname; }
    public void setAccountname(String v) { this.accountname = v; }
    
    public String getEmailaddress() { return emailaddress; }
    public void setEmailaddress(String v) { this.emailaddress = v; }
    
    public String getPhonenumber() { return phonenumber; }
    public void setPhonenumber(String v) { this.phonenumber = v; }
    
    public PrimaryAddress getPrimaryaddress() { return primaryaddress; }
    public void setPrimaryaddress(PrimaryAddress v) { this.primaryaddress = v; }

    // Nested AccountHolder class
    public static class AccountHolder {
        private String dateofbirth;
        private String firstname;
        private String lastname;

        public String getDateofbirth() { return dateofbirth; }
        public void setDateofbirth(String v) { this.dateofbirth = v; }
        
        public String getFirstname() { return firstname; }
        public void setFirstname(String v) { this.firstname = v; }
        
        public String getLastname() { return lastname; }
        public void setLastname(String v) { this.lastname = v; }
    }

    // Nested PrimaryAddress class - different from the other Address!
    public static class PrimaryAddress {
        private String addressline1;
        private String city;
        private String country;
        private String postalcode;
        private String state;

        public String getAddressline1() { return addressline1; }
        public void setAddressline1(String v) { this.addressline1 = v; }

        public String getCity() { return city; }
        public void setCity(String v) { this.city = v; }
        
        public String getCountry() { return country; }
        public void setCountry(String v) { this.country = v; }
        
        public String getPostalcode() { return postalcode; }
        public void setPostalcode(String v) { this.postalcode = v; }
        
        public String getState() { return state; }
        public void setState(String v) { this.state = v; }
    }
}
