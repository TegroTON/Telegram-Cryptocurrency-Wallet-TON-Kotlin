package money.tegro.bnb;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Future;

/**
 * Created by CenterPrime on 2020/09/19.
 */
public class Erc20TokenWrapper extends Contract{

    private static final String ABI_JSON = "[{\"constant\":true,\"inputs\":[],\"name\":\"name\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_spender\",\"type\":\"address\"},{\"name\":\"_value\",\"type\":\"uint256\"}],\"name\":\"approve\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"totalSupply\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_from\",\"type\":\"address\"},{\"name\":\"_to\",\"type\":\"address\"},{\"name\":\"_value\",\"type\":\"uint256\"}],\"name\":\"transferFrom\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"decimals\",\"outputs\":[{\"name\":\"\",\"type\":\"uint8\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"_owner\",\"type\":\"address\"}],\"name\":\"balanceOf\",\"outputs\":[{\"name\":\"balance\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"symbol\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_to\",\"type\":\"address\"},{\"name\":\"_value\",\"type\":\"uint256\"}],\"name\":\"transfer\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"_owner\",\"type\":\"address\"},{\"name\":\"_spender\",\"type\":\"address\"}],\"name\":\"allowance\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"owner\",\"type\":\"address\"},{\"indexed\":true,\"name\":\"spender\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"Approval\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"from\",\"type\":\"address\"},{\"indexed\":true,\"name\":\"to\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"value\",\"type\":\"uint256\"}],\"name\":\"Transfer\",\"type\":\"event\"}]";

    protected Erc20TokenWrapper(String contractAddress, Web3j web3j, TransactionManager transactionManager){
        super(ABI_JSON, contractAddress, web3j, transactionManager, BigInteger.ZERO, BigInteger.ZERO);
    }

    protected Erc20TokenWrapper(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit){
        super(ABI_JSON, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected Erc20TokenWrapper(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit){
        super(ABI_JSON, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    public static Erc20TokenWrapper load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit){
        return new Erc20TokenWrapper(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static Erc20TokenWrapper load(String contractAddress, Web3j web3j, TransactionManager transactionManager){
        return new Erc20TokenWrapper(contractAddress, web3j, transactionManager);
    }

    public Utf8String name() throws IOException{
        Function function = new Function("name",
                Arrays.asList(),
                Arrays.asList(new TypeReference<Utf8String>(){
                }));
        return executeCallSingleValueReturn(function);
    }

    public Future<Uint256> totalSupply() throws IOException{
        Function function = new Function("totalSupply",
                Arrays.asList(),
                Arrays.asList(new TypeReference<Uint256>(){
                }));
        return executeCallSingleValueReturn(function);
    }

    public Uint8 decimals() throws IOException{
        Function function = new Function("decimals",
                Arrays.asList(),
                Arrays.asList(new TypeReference<Uint8>(){
                }));
        return executeCallSingleValueReturn(function);
    }

    public Utf8String symbol() throws IOException{
        Function function = new Function("symbol",
                Arrays.asList(),
                Arrays.asList(new TypeReference<Utf8String>(){
                }));
        return executeCallSingleValueReturn(function);
    }

    public Uint256 balanceOf(Address _owner) throws IOException{
        Function function = new Function("balanceOf",
                Arrays.asList(_owner),
                Arrays.asList(new TypeReference<Uint256>(){
                }));
        return executeCallSingleValueReturn(function);
    }

    public TransactionReceipt transfer(Address _to, Uint256 _amount) throws IOException, TransactionException{
        Function function = new Function("transfer", Arrays.asList(_to, _amount), Collections.emptyList());
        return executeTransaction(function);
    }

}
