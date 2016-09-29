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

	private String ACCESS_TOKEN;
	private String REFRESH_TOKEN;
	private String VIEW_ID;
	
	private final String CLIENT_SECRET = "client_secrets.json";
	private final String APPLICATION_NAME = "TheLandscape.io";
	private NetHttpTransport HTTP_TRANSPORT;
	private JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	
	public void setAccessToken(String accessToken){
		this.ACCESS_TOKEN = accessToken;
	}
	
	public String getAccessToken(){
		return this.ACCESS_TOKEN;
	}
	
	public void setRefreshToken(String refreshToken){
		this.REFRESH_TOKEN = refreshToken;
	}
	
	public String getRefreshToken(){
		return this.REFRESH_TOKEN;
	}

	public void setViewID(String viewID){
		this.VIEW_ID = viewID;
	}
	
	public String getViewID(){
		return this.VIEW_ID;
	}
	
	public String run(String accessToken, String refreshToken, String viewID) throws GeneralSecurityException, IOException{
		System.out.println("Running...");
		GoogleCredential credential = setUpCredentials(accessToken, refreshToken);
		HttpURLConnection request = setUpRequest(accessToken);
		checkRequest(request,credential,accessToken);
		return runQuery(this.getAccessToken(),this.getViewID());
		
	}
	
	private GoogleCredential setUpCredentials(String accessToken, String refreshToken) throws GeneralSecurityException, IOException{
		try{
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,new InputStreamReader(Combined.class.getResourceAsStream(CLIENT_SECRET)));
			GoogleCredential credential = new GoogleCredential.Builder()
				.setClientSecrets(clientSecrets)
				.setTransport(HTTP_TRANSPORT)
				.setJsonFactory(JSON_FACTORY)
				.build();
					
			credential.setAccessToken(accessToken);
			credential.setRefreshToken(refreshToken);
			System.out.println("We set up credentials!");
			return credential;
		}catch(IOException e){
			System.out.println("Could not set up credentials!");
			System.out.println("Error : " + e.getMessage());
			return null;
		}
	}
	
	private HttpURLConnection setUpRequest(String accessToken) throws GeneralSecurityException, IOException{
		try{
			String token_check_url = "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + accessToken;
			URL url = new URL(token_check_url);
			HttpURLConnection request = (HttpURLConnection) url.openConnection();
			request.connect();
			System.out.println("We set up and connected a request!");
			return request;
		}catch(IOException e){
			System.out.println("Could not set up a request!");
			System.out.println("Error : " + e.getMessage());
			return null;
		}
	}
	
	private String checkRequest(HttpURLConnection request, GoogleCredential credential, String accessToken) throws GeneralSecurityException, IOException{
		try{
			request.getContent();
			JsonParser jp = new JsonParser();
			JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
			JsonObject rootobj = root.getAsJsonObject();
			int exp_time = rootobj.get("expires_in").getAsInt();
			
			System.out.println("We got token info!");
			
			// If token is not expired but has 60 seconds or less left, refresh. Anything more and we just return the token we started with.
			if(exp_time <= 60){
				System.out.println("Token is going to expire in less than 60 seconds. Sending token to refresh()");
				this.setAccessToken(refreshAccessToken(credential));
				return this.getAccessToken();
			}else{
				System.out.println("Token is fine! Returning original access token");
				return this.getAccessToken();
			}
			
		}catch(IOException e){
			// We received a request error, send to refresh
			System.out.println("We received an error, Most likely expired! Sending token to refresh()");
			String newToken = refreshAccessToken(credential);
			if(newToken == null){
				System.out.println("Failed to get new token");
				return null;
			}else{				
				this.setAccessToken(newToken);
				return this.getAccessToken();
			}
		}
	}
	
	private String refreshAccessToken(GoogleCredential credential) throws IOException{
		try{
			System.out.println("Running refreshAccessToken and returning a new Access Token!");
			credential.refreshToken();
			this.setAccessToken(credential.getAccessToken());
			System.out.println("New access token is : " + this.getAccessToken());
			return this.getAccessToken();
		}catch(IOException e){
			System.out.println("Token was not able to be refreshed either because the access token is revoked/invalid or because the refresh token is wrong.");
			return null;
		}
	}
	
	private String runQuery(String accessToken, String viewID) throws GeneralSecurityException, IOException{
		try{
			System.out.println("Trying to run the query!");
			AnalyticsReporting service = initializeAnalyticsReporting(accessToken);
			GetReportsResponse response = getReport(service);
			printResponse(response,this.getViewID());
			return "Ran";
		}catch(IOException e){
			System.out.println("Could not run the query");
			return ("Error : " + e.getMessage());
		}
	}
	
	private AnalyticsReporting initializeAnalyticsReporting(String accessToken) throws GeneralSecurityException, IOException {
		HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accessToken);
		return new AnalyticsReporting.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
	  }

	private GetReportsResponse getReport(AnalyticsReporting service) throws IOException {
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
			.setViewId(this.VIEW_ID)
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

	private void printResponse(GetReportsResponse response,String viewID) {
		for (Report report: response.getReports()) {
			ColumnHeader header = report.getColumnHeader();
			List<String> dimensionHeaders = header.getDimensions();
			List<MetricHeaderEntry> metricHeaders = header.getMetricHeader().getMetricHeaderEntries();
			List<ReportRow> rows = report.getData().getRows();

			if (rows == null) {
				System.out.println("No data found for " + viewID);
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

	
	
	public static void main(String[] args) throws GeneralSecurityException, IOException {
		
		Combined analytics = new Combined();
		
		analytics.setAccessToken("");
		analytics.setRefreshToken("");
		analytics.setViewID("");
		
		String accessToken = analytics.getAccessToken();
		String refreshToken = analytics.getRefreshToken();
		String viewID = analytics.getViewID();
		
		analytics.run(accessToken,refreshToken,viewID);
		
	}

}
