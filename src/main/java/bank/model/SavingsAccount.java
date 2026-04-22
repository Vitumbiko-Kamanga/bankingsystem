package bank.model;

public class SavingsAccount extends Account {

    public SavingsAccount(String id, String accNo, String owner, double balance) {
        super(id, accNo, owner, balance);
    }

    @Override
    public String getAccountType() {
        return "Savings";
    }
}