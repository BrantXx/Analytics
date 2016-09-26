import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.analyticsreporting.v4.AnalyticsReporting;
import com.google.api.services.analyticsreporting.v4.model.ColumnHeader;
import com.google.api.services.analyticsreporting.v4.model.DateRange;
import com.google.api.services.analyticsreporting.v4.model.DateRangeValues;
import com.google.api.services.analyticsreporting.v4.model.Dimension;
import com.google.api.services.analyticsreporting.v4.model.GetReportsRequest;
import com.google.api.services.analyticsreporting.v4.model.GetReportsResponse;
import com.google.api.services.analyticsreporting.v4.model.Metric;
import com.google.api.services.analyticsreporting.v4.model.MetricHeaderEntry;
import com.google.api.services.analyticsreporting.v4.model.Report;
import com.google.api.services.analyticsreporting.v4.model.ReportRequest;
import com.google.api.services.analyticsreporting.v4.model.ReportRow;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Combined {
	// ACCESS_TOKEN, REFRESH_TOKEN, and VIEW_ID need to come from the database.
	private static String ACCESS_TOKEN = "";
	private static String REFRESH_TOKEN = "";
	private static final String VIEW_ID = "";
	
	private static final String CLIENT_SECRET = "client_secrets.json";
	private static final String APPLICATION_NAME = "TheLandscape.io";
	private static NetHttpTransport HTTP_TRANSPORT;
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	

	public static void main(String[] args) {
		try{
			// Set up the credential, and the request.
			// The credential will hold the access token and refresh token
			// The request will check if the access token is valid(if it has expires_in) or invalid(revoked,expired with a 400 "Bad Request"
			// If the token is 400'd then try to get another token from the refresh token. If no access token is returned from the refresh token, it was revoked or the token isn't real.
			// If the token is refreshed and a new access token is provided, return the new token(we'll also need to store this token for future checks)
			// Send the returnFromCheck to checkBeforeSendTokenToQuery which will hold the string "revoked" if invalid, or it will hold the new access token if the refresh was successful.
			// If revoked, ??
			// If access token, then send it to PassTokenToQuery which will set up the Analytics object with the access token, send the object to getReports(query), and print the response.
			
			GoogleCredential credential = setUpCredentials();
			HttpURLConnection request = setUpRequest();
			String returnFromCheck = checkTokenFromRequest(request,credential);
			checkBeforeSendTokenToQuery(returnFromCheck);
			
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	private static String checkBeforeSendTokenToQuery(String accessToken) throws GeneralSecurityException, IOException{
		if(accessToken == "revoked"){
			System.out.println("access_token was revoked by the user");
			return "access_token was revoked by the user";
		}else{
			passTokenToQuery(accessToken);
			return "Sent Token to Query";
		}
	}
	
	private static String checkTokenFromRequest(HttpURLConnection request, GoogleCredential credential) throws GeneralSecurityException, IOException{
		if(request.getResponseCode() == 400){
			// Token is Invalid(Expired or Revoked). Refresh it anyway, if we get an access token back, it was expired, if not its been revoked. 
			credential.refreshToken();
			if(credential.getAccessToken() == null){
				return "revoked";
			}else{
				// Update the old token in the database with the new token.
				// passTokenToQuery(credential.getAccessToken());
				// return "new_access_token";
			}
		}else{
			JsonParser jp = new JsonParser();
			JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
			JsonObject rootobj = root.getAsJsonObject();
			int exp_time = rootobj.get("expires_in").getAsInt();
			if(exp_time <= 60){
				credential.refreshToken();
				// Update the old token in the database with the new token.
				// passTokenToQuery(credential.getAccessToken());
				// return "new_access_token";
			}else{
				passTokenToQuery(ACCESS_TOKEN);
				return ACCESS_TOKEN;
			}
		}
		return "fail";
	}
	
	private static GoogleCredential setUpCredentials() throws GeneralSecurityException, IOException{
		HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,new InputStreamReader(Combined.class.getResourceAsStream(CLIENT_SECRET)));
		GoogleCredential credential = new GoogleCredential.Builder()
			.setClientSecrets(clientSecrets)
			.setTransport(HTTP_TRANSPORT)
			.setJsonFactory(JSON_FACTORY)
			.build();
		
		credential.setAccessToken(ACCESS_TOKEN);
		credential.setRefreshToken(REFRESH_TOKEN);
		return credential;
	}
	
	private static HttpURLConnection setUpRequest() throws GeneralSecurityException, IOException{
		String token_check_url = "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + ACCESS_TOKEN;
		URL url = new URL(token_check_url);
		HttpURLConnection request = (HttpURLConnection) url.openConnection();
		request.connect();
		return request;
	}
	
	private static void passTokenToQuery(String accessToken) throws GeneralSecurityException, IOException{
		AnalyticsReporting service = initializeAnalyticsReporting(accessToken);
		GetReportsResponse response = getReport(service);
		printResponse(response);
	}
	
	private static AnalyticsReporting initializeAnalyticsReporting(String accessToken) throws GeneralSecurityException, IOException {
		HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accessToken);
		return new AnalyticsReporting.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
	  }

	private static GetReportsResponse getReport(AnalyticsReporting service) throws IOException {
		DateRange dateRange = new DateRange();
		dateRange.setStartDate("30DaysAgo");
		dateRange.setEndDate("today");

		Metric sessions = new Metric()
			.setExpression("ga:sessions")
			.setAlias("sessions");
		
		Dimension landingpage = new Dimension()
			.setName("ga:landingPagePath");
		
		Dimension medium = new Dimension()
		.setName("ga:medium");
		
		ReportRequest request = new ReportRequest()
			.setViewId(VIEW_ID)
			.setDateRanges(Arrays.asList(dateRange))
			.setDimensions(Arrays.asList(landingpage,medium))
			.setMetrics(Arrays.asList(sessions))
			.setFiltersExpression("ga:medium==organic");
		
		ArrayList<ReportRequest> requests = new ArrayList<ReportRequest>();
		requests.add(request);

		GetReportsRequest getReport = new GetReportsRequest()
			.setReportRequests(requests);

		GetReportsResponse response = service.reports().batchGet(getReport).execute();

		return response;
	}

	private static void printResponse(GetReportsResponse response) {	  
		for (Report report: response.getReports()) {
			ColumnHeader header = report.getColumnHeader();
			List<String> dimensionHeaders = header.getDimensions();
			List<MetricHeaderEntry> metricHeaders = header.getMetricHeader().getMetricHeaderEntries();
			List<ReportRow> rows = report.getData().getRows();

			if (rows == null) {
				System.out.println("No data found for " + VIEW_ID);
				return;
			}
				
			for (ReportRow row: rows) {
				List<String> dimensions = row.getDimensions();
				List<DateRangeValues> metrics = row.getMetrics();

				for (int i = 0; i < dimensionHeaders.size() && i < dimensions.size(); i++) {
					System.out.println(dimensionHeaders.get(i) + ": " + dimensions.get(i));
				}

				for (int j = 0; j < metrics.size(); j++) {
					DateRangeValues values = metrics.get(j);
					for (int k = 0; k < values.getValues().size() && k < metricHeaders.size(); k++) {
						System.out.println(metricHeaders.get(k).getName() + ": " + values.getValues().get(k));
					}
				}
			}

			DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
			Calendar cal = Calendar.getInstance();
			String currentDate = dateFormat.format(cal.getTime());
			cal.add(Calendar.MONTH, -1);
			String lastMonthDate = dateFormat.format(cal.getTime());
			System.out.println("From " + lastMonthDate + " to " + currentDate);		
  		}
	}
}
