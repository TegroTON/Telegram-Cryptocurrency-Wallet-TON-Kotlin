package money.tegro.bnb;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.*;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Created by CenterPrime on 2020/09/19.
 */
public class CenterPrimeUtils extends WalletUtils{
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static{
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static String generateFullNewWalletFile(String password, File destinationDirectory)
            throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidAlgorithmParameterException, CipherException, IOException{

        return generateNewWalletFile(password, destinationDirectory, true);
    }

    public static String generateLightNewWalletFile(String password, File destinationDirectory)
            throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidAlgorithmParameterException, CipherException, IOException{

        return generateNewWalletFile(password, destinationDirectory, false);
    }

    public static String generateNewWalletFile(
            String password, File destinationDirectory, boolean useFullScrypt)
            throws CipherException, IOException, InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, NoSuchProviderException{

        ECKeyPair ecKeyPair = Keys.createEcKeyPair();
        return generateWalletFile(password, ecKeyPair, destinationDirectory, useFullScrypt);
    }

    public static String generateWalletFile(
            String password, ECKeyPair ecKeyPair, File destinationDirectory, boolean useFullScrypt)
            throws CipherException, IOException{

        WalletFile walletFile;
        if(useFullScrypt){
            walletFile = Wallet.createStandard(password, ecKeyPair);
        }else{
            walletFile = Wallet.createLight(password, ecKeyPair);
        }

        String fileName = getWalletFileName(walletFile);
        File destination = new File(destinationDirectory, fileName.toLowerCase());
        objectMapper.writeValue(destination, walletFile);
        return fileName;
    }

//    public static Credentials loadCredentials(String password, String source)
//            throws IOException, CipherException {
//        return loadCredentials(password, new File(source));
//    }

    public static Credentials loadCredentials(String password, String keystore)
            throws IOException, CipherException{
        WalletFile walletFile = objectMapper.readValue(keystore, WalletFile.class);
        return Credentials.create(Wallet.decrypt(password, walletFile));
    }


    private static String getWalletFileName(WalletFile walletFile){
        return walletFile.getAddress();
    }

}

