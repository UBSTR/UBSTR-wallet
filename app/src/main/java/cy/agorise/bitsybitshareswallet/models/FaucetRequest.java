package cy.agorise.bitsybitshareswallet.models;

/**
 * Class used to encapsulate a faucet account creation request
 */

public class FaucetRequest {
    private FaucetAccount account;

    public FaucetRequest(String accountName, String address, String referrer){
        account = new FaucetAccount(accountName, address, referrer);
    }
}
