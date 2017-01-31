package denominator.ultradns.model;

import java.util.List;

public class AccountList {

    private List<Account> accounts;

    public List<Account> getAccounts() {
        return accounts;
    }

    public class Account {

        private String accountName;

        public String getAccountName() {
            return accountName;
        }
    }
}
