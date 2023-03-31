package money.tegro.bnb;

import io.reactivex.Single;
import org.bouncycastle.util.encoders.Hex;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.response.NoOpProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by CenterPrime on 2020/09/19.
 */
public class BinanceManager{

    private static final BinanceManager ourInstance = new BinanceManager();

    /**
     * getWeb3j() Client
     */
    private Web3j[] web3j;
    private AtomicInteger web3jRoundRobin = new AtomicInteger(0);

    /**
     * Hyperledger Api
     */
    private HyperLedgerApi hyperLedgerApi;

    /**
     * Network node
     */
    private Network network = Network.MAINNET;

    public BinanceManager(){
        this(Network.MAINNET);
    }

    public BinanceManager(Network network){
        init(network);
    }

    public static BinanceManager getInstance(){
        return ourInstance;
    }

    /**
     * Initialize EthManager
     *
     * @param network : Network
     */
    public void init(Network network){
        this.network = network;

        List<String> urls = network.getUrls();
        web3j = new Web3j[urls.size()];
        for(int i = 0; i < urls.size(); i++){
            web3j[i] = Web3j.build(new HttpService(urls.get(i)));
        }
    }

    /**
     * Get Current Gas Price
     */
    public BigInteger getGasPrice(){
        try{
            EthGasPrice price = getWeb3j().ethGasPrice()
                    .send();
            return price.getGasPrice();
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return new BigInteger(Const.DEFAULT_GAS_PRICE);
    }


    /**
     * Create Wallet by password
     */
    public Single<Wallet> createWallet(String password){
        return Single.fromCallable(() -> {
            HashMap<String, Object> body = new HashMap<>();
            body.put("network", isMainNet() ? "MAINNET" : "TESTNET");
            try{

                String walletAddress = CenterPrimeUtils.generateNewWalletFile(password, new File(""), false);
                String walletPath = walletAddress.toLowerCase();
                File keystoreFile = new File(walletPath);
                String keystore = read_file(keystoreFile.getName());

                body.put("action_type", "WALLET_CREATE");
                body.put("wallet_address", walletAddress);
                body.put("status", "SUCCESS");
                sendEventToLedger(body);
                return new Wallet(walletAddress, keystore);
            }catch(CipherException | IOException | NoSuchAlgorithmException | InvalidAlgorithmParameterException |
                   NoSuchProviderException e){
                e.printStackTrace();
                body.put("status", "FAILURE");
            }
            sendEventToLedger(body);
            return null;
        });
    }

    /**
     * Export Keystore by wallet address
     */
    public Single<String> exportKeyStore(String walletAddress){
        return Single.fromCallable(() -> {
            String wallet = walletAddress;
            if(wallet.startsWith("0x")){
                wallet = wallet.substring(2);
            }
            String walletPath = wallet.toLowerCase();
            File keystoreFile = new File(walletPath);
            HashMap<String, Object> body = new HashMap<>();
            body.put("network", isMainNet() ? "MAINNET" : "TESTNET");
            if(keystoreFile.exists()){

                body.put("action_type", "WALLET_EXPORT_KEYSTORE");
                body.put("wallet_address", walletAddress);
                body.put("status", "SUCCESS");
                sendEventToLedger(body);
                return read_file(keystoreFile.getName());
            }else{
                body.put("action_type", "WALLET_EXPORT_KEYSTORE");
                body.put("wallet_address", walletAddress);
                body.put("status", "FAILURE");
                throw new Exception("Keystore is NULL");
            }
        });
    }

    /**
     * Get Keystore by wallet address
     */
    public Single<String> getKeyStore(String walletAddress){
        return Single.fromCallable(() -> {
            String wallet = walletAddress;
            if(wallet.startsWith("0x")){
                wallet = wallet.substring(2);
            }
            String walletPath = wallet.toLowerCase();
            File keystoreFile = new File(walletPath);
            if(keystoreFile.exists()){
                return read_file(keystoreFile.getName());
            }else{
                throw new Exception("Keystore is NULL");
            }
        });
    }

    /**
     * Import Wallet by Keystore
     */
    public Single<String> importFromKeystore(String keystore, String password){
        return Single.fromCallable(() -> {
            HashMap<String, Object> body = new HashMap<>();
            body.put("network", isMainNet() ? "MAINNET" : "TESTNET");
            try{
                Credentials credentials = CenterPrimeUtils.loadCredentials(password, keystore);
                String walletAddress = CenterPrimeUtils.generateWalletFile(password, credentials.getEcKeyPair(), new File(""), false);

                body.put("action_type", "WALLET_IMPORT_KEYSTORE");
                body.put("wallet_address", walletAddress);
                body.put("status", "SUCCESS");
                sendEventToLedger(body);
                return walletAddress;
            }catch(IOException e){
                body.put("action_type", "WALLET_IMPORT_KEYSTORE");
                body.put("status", "FAILURE");
                sendEventToLedger(body);
                e.printStackTrace();
            }
            sendEventToLedger(body);
            return null;
        });
    }

    /**
     * Import Wallet with Private Key
     */
    public Single<String> importFromPrivateKey(String privateKey){
        return Single.fromCallable(() -> {
            HashMap<String, Object> body = new HashMap<>();
            body.put("network", isMainNet() ? "MAINNET" : "TESTNET");
            String password = "BinanceSDK";
            try{
                Credentials credentials = getCredentials(Hex.decode(privateKey));
                String walletAddress = CenterPrimeUtils.generateWalletFile(password, credentials.getEcKeyPair(), new File(""), false);

                System.out.println("cred address: " + credentials.getAddress());
                System.out.println("wallet address: " + walletAddress);

                body.put("action_type", "WALLET_IMPORT_PRIVATE_KEY");
                body.put("wallet_address", walletAddress);
                body.put("status", "SUCCESS");
                sendEventToLedger(body);
                return walletAddress;
            }catch(CipherException | IOException e){
                e.printStackTrace();
                body.put("action_type", "WALLET_IMPORT_PRIVATE_KEY");
                body.put("status", "FAILURE");
                sendEventToLedger(body);
                return "";
            }
        });
    }

    /**
     * Export Private Key
     */
    public Single<String> exportPrivateKey(String walletAddress, String password){
        return loadCredentials(walletAddress, password)
                .flatMap(credentials -> {
                    String privateKey = credentials.getEcKeyPair().getPrivateKey().toString(16);

                    HashMap<String, Object> body = new HashMap<>();
                    body.put("network", isMainNet() ? "MAINNET" : "TESTNET");
                    body.put("action_type", "WALLET_EXPORT_PRIVATE_KEY");
                    body.put("wallet_address", walletAddress);
                    body.put("status", "SUCCESS");
                    sendEventToLedger(body);

                    return Single.just(privateKey);
                });
    }

    /**
     * Get BNB Balance of Wallet
     */
    public Single<BigDecimal> getBNBBalance(String address){
        return Single.fromCallable(() -> {

            BigInteger valueInWei = getWeb3j()
                    .ethGetBalance(address, DefaultBlockParameterName.LATEST)
                    .send()
                    .getBalance();
            return BalanceUtils.weiToEth(valueInWei);
        });
    }


    /**
     * Load Credentials
     */
    public Single<Credentials> loadCredentials(String walletAddress, String password){
        return getKeyStore(walletAddress)
                .flatMap(keystore -> {
                    try{
                        Credentials credentials = CenterPrimeUtils.loadCredentials(password, keystore);
                        return Single.just(credentials);
                    }catch(IOException e){
                        e.printStackTrace();
                        return Single.error(e);
                    }catch(CipherException e){
                        e.printStackTrace();
                        return Single.error(e);
                    }
                });
    }

    public Credentials getCredentials(byte[] privateKey){
        ECKeyPair keys = ECKeyPair.create(privateKey);
        return Credentials.create(keys);
    }

    /**
     * Add Custom Token
     */
    public Single<Token> searchTokenByContractAddress(String contractAddress, String walletAddress, String password){
        return loadCredentials(walletAddress, password)
                .flatMap(credentials -> {
                    TransactionReceiptProcessor transactionReceiptProcessor = new NoOpProcessor(getWeb3j());
                    TransactionManager transactionManager = new RawTransactionManager(
                            getWeb3j(), credentials, isMainNet() ? (byte) 56 : (byte) 97, transactionReceiptProcessor);
                    Erc20TokenWrapper contract = Erc20TokenWrapper.load(contractAddress, getWeb3j(), transactionManager, BigInteger.ZERO, BigInteger.ZERO);
                    String tokenName = contract.name().getValue();
                    String tokenSymbol = contract.symbol().getValue();
                    BigInteger decimalCount = contract.decimals().getValue();

                    return Single.just(new Token(contractAddress, tokenSymbol, tokenName, decimalCount.toString()));
                });

    }

    /**
     * Get BEP20 Token Balance of Wallet
     */
    public Single<BigDecimal> getTokenBalance(String walletAddress, String password, String tokenContractAddress){
        return loadCredentials(walletAddress, password)
                .flatMap(credentials -> {

                    TransactionReceiptProcessor transactionReceiptProcessor = new NoOpProcessor(getWeb3j());
                    TransactionManager transactionManager = new RawTransactionManager(
                            getWeb3j(), credentials, isMainNet() ? (byte) 56 : (byte) 97, transactionReceiptProcessor);
                    Erc20TokenWrapper contract = Erc20TokenWrapper.load(tokenContractAddress, getWeb3j(),
                            transactionManager, BigInteger.ZERO, BigInteger.ZERO);
                    Address address = new Address(walletAddress);
                    BigInteger tokenBalance = contract.balanceOf(address).getValue();
                    String tokenName = contract.name().getValue();
                    String tokenSymbol = contract.symbol().getValue();
                    BigInteger decimalCount = contract.decimals().getValue();

                    BigDecimal tokenValueByDecimals = BalanceUtils.balanceByDecimal(tokenBalance, decimalCount);

                    HashMap<String, Object> body = new HashMap<>();
                    body.put("action_type", "TOKEN_BALANCE");
                    body.put("wallet_address", walletAddress);
                    body.put("token_smart_contract", tokenContractAddress);
                    body.put("token_name", tokenName);
                    body.put("token_symbol", tokenSymbol);
                    body.put("network", isMainNet() ? "MAINNET" : "TESTNET");
                    body.put("balance", tokenValueByDecimals.doubleValue());
                    body.put("status", "SUCCESS");
                    sendEventToLedger(body);

                    return Single.just(tokenValueByDecimals);
                });
    }

    /**
     * Send BNB
     */
    public Single<String> sendBNB(Credentials credentials,
                                  BigInteger gasPrice,
                                  BigInteger gasLimit,
                                  BigDecimal bnbAmount,
                                  String to_Address){
        return Single.just(credentials).flatMap(c -> {
            String walletAddress = c.getAddress();
            BigInteger nonce = getNonce(walletAddress);
            System.out.println("nonce: " + nonce);
            BigDecimal weiValue = Convert.toWei(bnbAmount, Convert.Unit.ETHER);
            System.out.println("wei " + weiValue);

            RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                    nonce, gasPrice, gasLimit, to_Address, weiValue.toBigIntegerExact());
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            // 0xf86680148255f094cc033410c14a9209f580763c081c72d39a833c8f872386f26fc10000801ba0ed4ea7f037fb2593392e62e7396d2c47a5cf63892b8ed91ff390d6ba07a7397ba00df101a996bf7ffee517ae7a16563d59f3d2af2b3c764f00e92aa25b37d43db2
            // 0xf86680148255f094cc033410c14a9209f580763c081c72d39a833c8f872386f26fc10000801ba0ed4ea7f037fb2593392e62e7396d2c47a5cf63892b8ed91ff390d6ba07a7397ba00df101a996bf7ffee517ae7a16563d59f3d2af2b3c764f00e92aa25b37d43db2
            System.out.println("raw transaction: " + hexValue);

            EthSendTransaction ethSendTransaction = getWeb3j().ethSendRawTransaction(hexValue).sendAsync().get();

            String transactionHash = ethSendTransaction.getTransactionHash();
            System.out.println("transaction: " + ethSendTransaction);
            System.out.println("transaction: " + ethSendTransaction.getTransactionHash());

            HashMap<String, Object> body = new HashMap<>();
            body.put("action_type", "SEND_BNB");
            body.put("from_wallet_address", walletAddress);
            body.put("to_wallet_address", to_Address);
            body.put("amount", bnbAmount.toPlainString());
            body.put("tx_hash", transactionHash);
            body.put("gasLimit", gasLimit.toString());
            body.put("gasPrice", gasPrice.toString());
            body.put("fee", gasLimit.multiply(gasPrice).toString());
            body.put("network", isMainNet() ? "MAINNET" : "TESTNET");
            body.put("status", "SUCCESS");
            sendEventToLedger(body);


            return Single.just(transactionHash);
        });
    }

    /**
     * Send Token
     */
    public Single<String> sendToken(String walletAddress, String password,
                                    BigInteger gasPrice,
                                    BigInteger gasLimit,
                                    BigDecimal tokenAmount,
                                    String to_Address,
                                    String tokenContractAddress
    ){
        return loadCredentials(walletAddress, password)
                .flatMap(credentials -> {
                    TransactionReceiptProcessor transactionReceiptProcessor = new NoOpProcessor(getWeb3j());
                    TransactionManager transactionManager = new RawTransactionManager(
                            getWeb3j(), credentials, isMainNet() ? (byte) 56 : (byte) 97, transactionReceiptProcessor);
                    Erc20TokenWrapper contract = Erc20TokenWrapper.load(tokenContractAddress, getWeb3j(), transactionManager, gasPrice, gasLimit);

                    String tokenName = contract.name().getValue();
                    String tokenSymbol = contract.symbol().getValue();
                    BigInteger decimalCount = contract.decimals().getValue();

                    BigDecimal formattedAmount = BalanceUtils.amountByDecimal(tokenAmount, new BigDecimal(decimalCount));

                    TransactionReceipt mReceipt = contract.transfer(new Address(to_Address), new Uint256(formattedAmount.toBigInteger()));

                    HashMap<String, Object> body = new HashMap<>();
                    body.put("action_type", "SEND_TOKEN");
                    body.put("from_wallet_address", walletAddress);
                    body.put("to_wallet_address", to_Address);
                    body.put("amount", tokenAmount.toPlainString());
                    body.put("tx_hash", mReceipt.getTransactionHash());
                    body.put("gasLimit", gasLimit.toString());
                    body.put("gasPrice", gasPrice.toString());
                    body.put("fee", gasLimit.multiply(gasPrice).toString());
                    body.put("network", isMainNet() ? "MAINNET" : "TESTNET");
                    body.put("token_smart_contract", tokenContractAddress);

                    body.put("token_name", tokenName);
                    body.put("token_symbol", tokenSymbol);

                    body.put("status", "SUCCESS");
                    sendEventToLedger(body);

                    return Single.just(mReceipt.getTransactionHash());
                });
    }


    /**
     * Get Nonce for Current Wallet Address
     */
    protected BigInteger getNonce(String walletAddress) throws IOException{
        EthGetTransactionCount ethGetTransactionCount = getWeb3j().ethGetTransactionCount(
                walletAddress, DefaultBlockParameterName.PENDING).send();

        return ethGetTransactionCount.getTransactionCount();
    }

    public String read_file(String filename) throws IOException{
        FileInputStream fis = new FileInputStream(filename);
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        BufferedReader bufferedReader = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = bufferedReader.readLine()) != null){
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private void sendEventToLedger(HashMap<String, Object> map){
//        try{
//            SubmitTransactionModel submitTransactionModel = new SubmitTransactionModel();
//            submitTransactionModel.setTx_type("BINANCE");
//            submitTransactionModel.setUsername("user1");
//            submitTransactionModel.setOrgname("org1");
//
//            submitTransactionModel.setBody(map);
//            hyperLedgerApi.submitTransaction(submitTransactionModel)
//                    .subscribeOn(Schedulers.io())
//                    .subscribe((objectBaseResponse, throwable) -> {
////                        System.out.println(objectBaseResponse);
//                    });
//        }catch(Exception e){
//            e.printStackTrace();
//        }
    }

    public Network getNetwork(){
        return network;
    }

    private boolean isMainNet(){
        return network == Network.MAINNET;
    }

    public Web3j getWeb3j(){
        int roundRobin = web3jRoundRobin.getAndIncrement();
        if(roundRobin < 0){
            web3jRoundRobin.set(0);
            roundRobin = 0;
        }
        int index = roundRobin % web3j.length;
        return web3j[index];
    }

    public enum Network{
        MAINNET(
                "https://bsc-dataseed.binance.org",
                "https://bsc-dataseed1.defibit.io",
                "https://bsc-dataseed1.ninicoin.io",
                "https://bsc.nodereal.io"
        ),
        TESTNET(
                "https://data-seed-prebsc-1-s1.binance.org:8545"
//                "https://data-seed-prebsc-2-s1.binance.org:8545",
//                "https://data-seed-prebsc-1-s2.binance.org:8545",
//                "https://data-seed-prebsc-2-s2.binance.org:8545",
//                "https://data-seed-prebsc-1-s3.binance.org:8545",
//                "https://data-seed-prebsc-2-s3.binance.org:8545"
        );

        private final String[] urls;

        Network(String... urls){
            this.urls = urls;
        }

        public List<String> getUrls(){
            return List.of(urls);
        }
    }
}
