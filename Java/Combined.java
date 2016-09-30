import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.analyticsreporting.v4.AnalyticsReporting;
import com.google.api.services.analyticsreporting.v4.model.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;


public class Combined {
	private String accessToken;
	private String refreshToken;
	private String viewID;

	private NetHttpTransport httpTransport;
	private JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

	public void setAccessToken(String accessToken){
		this.accessToken = accessToken;
	}

	public String getAccessToken(){
		return this.accessToken;
	}

	public void setRefreshToken(String refreshToken){
		this.refreshToken = refreshToken;
	}

	public String getRefreshToken(){
		return this.refreshToken;
	}

	public void setViewID(String viewID){
		this.viewID = viewID;
	}

	private String getViewID(){
		return this.viewID;
	}

	public String run(String accessToken, String refreshToken) throws GeneralSecurityException, IOException {
		System.out.println("Running...");
		GoogleCredential credential = setUpCredentials(accessToken, refreshToken);
		HttpURLConnection request = setUpRequest(accessToken);
		checkRequest(request,credential);
		return runQuery();

	}

	private GoogleCredential setUpCredentials(String accessToken, String refreshToken) throws GeneralSecurityException, IOException{
		try{
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			String clientSecret = "client_secrets.json";
			GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory,new InputStreamReader(Combined.class.getResourceAsStream(clientSecret)));
			GoogleCredential credential = new GoogleCredential.Builder()
					.setClientSecrets(clientSecrets)
					.setTransport(httpTransport)
					.setJsonFactory(jsonFactory)
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

	private String checkRequest(HttpURLConnection request, GoogleCredential credential) throws GeneralSecurityException, IOException{
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

	private String runQuery() throws GeneralSecurityException, IOException{
		try{
			System.out.println("Trying to run the query!");
			AnalyticsReporting service = initializeAnalyticsReporting(this.getAccessToken());
			GetReportsResponse response = getReport(service);
			printResponse(response,this.getViewID());
			return "Ran";
		}catch(IOException e){
			System.out.println("Could not run the query");
			return ("Error : " + e.getMessage());
		}
	}

	private AnalyticsReporting initializeAnalyticsReporting(String accessToken) throws GeneralSecurityException, IOException {
		httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accessToken);
		String appName = "TheLandscape.io";
		return new AnalyticsReporting.Builder(httpTransport, jsonFactory, credential).setApplicationName(appName).build();
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
				.setViewId(this.viewID)
				.setDateRanges(Collections.singletonList(dateRange))
				.setDimensions(asList(landingpage,medium))
				.setMetrics(Collections.singletonList(sessions))
				.setFiltersExpression("ga:medium==organic");

		ArrayList<ReportRequest> requests = new ArrayList<>();
		requests.add(request);

		GetReportsRequest getReport = new GetReportsRequest()
				.setReportRequests(requests);

		return service.reports().batchGet(getReport).execute();
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

				for (DateRangeValues values : metrics) {
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

		analytics.run(accessToken,refreshToken);

	}

}
