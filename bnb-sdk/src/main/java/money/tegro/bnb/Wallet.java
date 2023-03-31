package money.tegro.bnb;

/**
 * Created by CenterPrime on 2020/11/04.
 */
public class Wallet{
    private String address;
    private String keystore;

    public Wallet(String address, String keystore){
        this.address = address;
        this.keystore = keystore;
    }

    public String getAddress(){
        return address;
    }

    public void setAddress(String address){
        this.address = address;
    }

    public String getKeystore(){
        return keystore;
    }

    public void setKeystore(String keystore){
        this.keystore = keystore;
    }
}
