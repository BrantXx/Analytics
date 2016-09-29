import static org.junit.Assert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.junit.Test;


public class CombinedTest {
		
	@Test
	public void testMain() throws GeneralSecurityException, IOException {
		Combined analytics = new Combined();
		
		String access_token = "1";
		String refresh_token = "2";
		String view_id = "3";
		
		analytics.setAccessToken(access_token);
		analytics.setRefreshToken(refresh_token);
		analytics.setViewID(view_id);
		
		String fromAnalytics = analytics.run(analytics.getAccessToken(),analytics.getRefreshToken(),analytics.getViewID());
		
		assertEquals(fromAnalytics,"Error : 401 Unauthorized");
		
	}

}

