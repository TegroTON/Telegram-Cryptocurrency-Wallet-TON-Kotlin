package money.tegro.bnb;

import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Created by CenterPrime on 2020/09/19.
 */
public class BalanceUtils{
    public static String weiInEth = "1000000000000000000";

    /**
     * Wei to Eth
     */
    public static BigDecimal weiToEth(BigInteger wei){
        return Convert.fromWei(new BigDecimal(wei), Convert.Unit.ETHER);
    }

    /**
     * Wei to Eth
     */
    public static BigDecimal weiToEth(BigDecimal wei){
        return Convert.fromWei(wei, Convert.Unit.ETHER);
    }

    /**
     * Eth to Wei
     */
    public static String ethToWei(String eth) throws Exception{
        BigDecimal wei = new BigDecimal(eth).multiply(new BigDecimal(weiInEth));
        return wei.toBigInteger().toString();
    }

    /**
     * Eth to Wei
     */
    public static BigDecimal ethToWei(BigDecimal amount) throws Exception{
        return amount.multiply(new BigDecimal(weiInEth));
    }

    /**
     * Wei to Gwei
     */
    public static String weiToGwei(BigInteger wei){
        return Convert.fromWei(new BigDecimal(wei), Convert.Unit.GWEI).toPlainString();
    }

    /**
     * Gwei to Wei
     */
    public static BigInteger gweiToWei(BigDecimal gwei){
        return Convert.toWei(gwei, Convert.Unit.GWEI).toBigInteger();
    }

    /**
     * Wei to String
     */
    public static String weiToString(String tokenAmount, int scale){
        BigDecimal value = new BigDecimal(tokenAmount).divide(new BigDecimal(weiInEth), scale, RoundingMode.FLOOR);
        return value.toPlainString();
    }

    /**
     * @param gasPrice : Gas Price amount
     * @param gasLimit : Gas Limit amount
     * @return calculate how much networkFee require
     */
    public static BigInteger networkFee(BigInteger gasPrice, BigInteger gasLimit){
        if(gasPrice != null && gasLimit != null){
            return gasPrice.multiply(gasLimit);
        }else{
            return BigInteger.ZERO;
        }
    }

    /**
     * Balance by decimals
     */
    public static BigDecimal balanceByDecimal(BigInteger balance, BigInteger decimals){
        BigDecimal tokenDecimals = new BigDecimal(Math.pow(10, decimals.intValue()));
        BigDecimal convertBalance = new BigDecimal(balance);
        return convertBalance.divide(tokenDecimals);
    }

    /**
     * Amount by decimals
     */
    public static BigDecimal amountByDecimal(BigDecimal amount, BigDecimal decimals){
        int convertDecimals = decimals.intValue();
        BigDecimal tokenDecimals = new BigDecimal("10");
        tokenDecimals = tokenDecimals.pow(convertDecimals);
        return amount.multiply(tokenDecimals);
    }


}
