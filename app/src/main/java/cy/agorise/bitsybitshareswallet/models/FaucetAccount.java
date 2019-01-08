package cy.agorise.bitsybitshareswallet.models;

/**
 * Class used to deserialize a the "account" object contained in the faucet response to the
 * {@link cy.agorise.bitsybitshareswallet.network.FaucetService#registerPrivateAccount(FaucetRequest)} API call.
 */

public class FaucetAccount {
    public String name;
    public String owner_key;
    public String active_key;
    public String memo_key;
    public String referrer;
    public String refcode;

    public FaucetAccount(String accountName, String address, String referrer){
        this.name = accountName;
        this.owner_key = address;
        this.active_key = address;
        this.memo_key = address;
        this.refcode = referrer;
        this.referrer = referrer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerKey() {
        return owner_key;
    }

    public void setOwnerKey(String owner_key) {
        this.owner_key = owner_key;
    }

    public String getActiveKey() {
        return active_key;
    }

    public void setActiveKey(String active_key) {
        this.active_key = active_key;
    }

    public String getMemoKey() {
        return memo_key;
    }

    public void setMemoKey(String memo_key) {
        this.memo_key = memo_key;
    }

    public String getRefcode() {
        return refcode;
    }

    public void setRefcode(String refcode) {
        this.refcode = refcode;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }
}

