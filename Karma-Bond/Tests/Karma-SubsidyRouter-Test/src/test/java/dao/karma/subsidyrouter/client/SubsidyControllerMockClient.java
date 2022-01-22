package dao.karma.subsidyrouter.client;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;

public class SubsidyControllerMockClient {
  
  public static void getSubsidyInfo(Score client, Account from) {
    client.invoke(from, "getSubsidyInfo");
  }
}
