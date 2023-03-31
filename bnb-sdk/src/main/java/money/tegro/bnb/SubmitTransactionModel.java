package money.tegro.bnb;

import java.util.HashMap;

public class SubmitTransactionModel{
    String orgname;
    String username;
    String tx_type;

    HashMap<String, Object> body;

    public SubmitTransactionModel(){
    }

    public SubmitTransactionModel(String orgname, String username, String tx_type, HashMap<String, Object> body){
        this.orgname = orgname;
        this.username = username;
        this.tx_type = tx_type;
        this.body = body;
    }

    public String getOrgname(){
        return orgname;
    }

    public void setOrgname(String orgname){
        this.orgname = orgname;
    }

    public String getUsername(){
        return username;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public String getTx_type(){
        return tx_type;
    }

    public void setTx_type(String tx_type){
        this.tx_type = tx_type;
    }

    public HashMap<String, Object> getBody(){
        return body;
    }

    public void setBody(HashMap<String, Object> body){
        this.body = body;
    }
}
