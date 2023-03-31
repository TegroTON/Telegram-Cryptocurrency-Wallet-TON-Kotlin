package money.tegro.bnb;

import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by CenterPrime on 2020/11/01.
 */
public interface HyperLedgerApi{
    @POST("/createTransaction/")
    Single<BaseResponse<Object>> submitTransaction(@Body SubmitTransactionModel submitTransactionModel);
}
