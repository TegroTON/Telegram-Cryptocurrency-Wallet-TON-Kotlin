package money.tegro.bnb;

public class Token{
    private String contractAddress;
    private String symbol;
    private String tokenName;
    private String decimals;

    public Token(String contractAddress, String symbol, String tokenName, String decimals){
        this.contractAddress = contractAddress;
        this.symbol = symbol;
        this.tokenName = tokenName;
        this.decimals = decimals;
    }


    public String getTokenName(){
        return tokenName;
    }

    public void setTokenName(String tokenName){
        this.tokenName = tokenName;
    }

    public String getContractAddress(){
        return contractAddress;
    }

    public void setContractAddress(String contractAddress){
        this.contractAddress = contractAddress;
    }

    public String getSymbol(){
        return symbol;
    }

    public void setSymbol(String symbol){
        this.symbol = symbol;
    }

    public String getDecimals(){
        return decimals;
    }

    public void setDecimals(String decimals){
        this.decimals = decimals;
    }
}
