package cy.agorise.bitsybitshareswallet.models;

public class FaucetResponse {
    public FaucetAccount account;
    public Error error;

    public class Error {
        public String[] base;
    }
}
