package com.github.andlyticsproject.console.v2;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.andlyticsproject.console.DevConsoleException;
import com.github.andlyticsproject.model.AppDetails;
import com.github.andlyticsproject.model.AppHistoricalStats;
import com.github.andlyticsproject.model.AppHistoricalStatsElement;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.Comment;

/**
 * This class contains static methods used to parse JSON from {@link DevConsoleV2}
 * 
 * See {@link https://github.com/AndlyticsProject/andlytics/wiki/Developer-Console-v2} for some more
 * documentation
 * 
 */
@SuppressWarnings("JavadocReference")
public class JsonParser {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = LoggerFactory.getLogger(JsonParser.class);

	private static final String TAG = JsonParser.class.getSimpleName();

	private static final boolean DEBUG = false;

	private JsonParser() {

	}

	/**
	 * Parses the supplied JSON string and adds the extracted ratings to the supplied
	 * {@link AppStats} object
	 * 
	 * @param json
	 * @param stats
	 * @throws JSONException
	 */
	static void parseRatings(String json, AppStats stats) throws JSONException {
		// Extract just the array with the values
		JSONObject values = new JSONObject(json).getJSONObject("result").getJSONArray("1")
				.getJSONObject(0);

		// Ratings are at index 2 - 6
		stats.setRating(values.getInt("2"), values.getInt("3"), values.getInt("4"),
				values.getInt("5"), values.getInt("6"));

	}

	/**
	 * Parses the supplied JSON string and adds the extracted statistics to the supplied
	 * {@link AppStats} object
	 * based on the supplied statsType
	 * Not used at the moment
	 * 
	 * @param json
	 * @param stats
	 * @param statsType
	 * @throws JSONException
	 */
	static void parseStatistics(String json, AppStats stats, int statsType) throws JSONException {
		// Extract the top level values array
		JSONObject values = new JSONObject(json).getJSONObject("result").getJSONObject("1");
		
		// For now we just care about todays value, later we may delve into the historical and
		// dimensioned data
		JSONArray historicalData = values.getJSONObject("1").getJSONArray("1");
		JSONObject latestData = historicalData.getJSONObject(historicalData.length() - 1);
		
		int latestValue = latestData.getJSONObject("2").getInt("1");
		if(stats.getHistoricalStats()==null){
			stats.setHistoricalStats(new AppHistoricalStats());
		}
		switch (statsType) {
			case DevConsoleV2Protocol.STATS_TYPE_TOTAL_USER_INSTALLS:
				stats.setTotalDownloads(latestValue);
				break;
			case DevConsoleV2Protocol.STATS_TYPE_DAILY_DEVICE_INSTALLS:
				
				stats.getHistoricalStats().setDailyInstallsByDevice(parseStatisticsHistoricalData(historicalData));
				break;
			case DevConsoleV2Protocol.STATS_TYPE_ACTIVE_DEVICE_INSTALLS:
				stats.setActiveInstalls(latestValue);
				break;
			default:
				break;
		}
		
		

	}
	static List<AppHistoricalStatsElement> parseStatisticsHistoricalData(JSONArray result) throws JSONException {
		// Extract the top level values array
		
		// For now we just care about todays value, later we may delve into the historical and
		// dimensioned data
		List<AppHistoricalStatsElement> elements=new ArrayList<AppHistoricalStatsElement>();
		for(int i=0;i<result.length();i++)
		{
			JSONObject obj=result.getJSONObject(i);
			long date=obj.getLong("1");
			String number=obj.getJSONObject("2").getString("1");			
			elements.add(new AppHistoricalStatsElement(new Date(date), number));
		}
		return elements;

	}
	
	/**
	 * Parses the supplied JSON string and builds a list of apps from it
	 * 
	 * @param json
	 * @param accountName
	 * @param skipIncomplete
	 * @return List of apps
	 * @throws JSONException
	 */
	static List<AppInfo> parseAppInfos(String json, String accountName,String developerId, boolean skipIncomplete)
			throws JSONException {

		
		List<AppInfo> apps = new ArrayList<AppInfo>();
		// Extract the base array containing apps
		JSONObject result = new JSONObject(json).getJSONObject("result");
		if (DEBUG) {
			pp("result", result);
		}

		JSONArray jsonApps = result.optJSONArray("1");
		if (DEBUG) {
			pp("jsonApps", jsonApps);
		}
		if (jsonApps == null) {
			// no apps yet?
			return apps;
		}

		int numberOfApps = jsonApps.length();
//		System.out.println(String.format("Found %d apps in JSON", numberOfApps));
		for (int i = 0; i < numberOfApps; i++) {
		
			JSONObject jsonApp = jsonApps.getJSONObject(i);
			AppInfo app=parseApp(jsonApp, skipIncomplete,developerId,accountName);      
			if(app!=null){
			apps.add(app);
			}
                        
		}

		return apps;
	}
	
	private static AppInfo parseApp(JSONObject jsonApp,boolean skipIncomplete,String developerId,String accountName) throws JSONException
	{
		Date now = new Date();
		AppInfo app = new AppInfo();
		app.setDeveloperId(developerId);
		app.setAccount(accountName);
		app.setLastUpdate(now);
		// Per app:
		// 1 : { 1: package name,
		// 2 : { 1: [{1 : lang, 2: name, 3: description, 4: ??, 5: what's new}], 2 : ?? },
		// 3 : ??,
		// 4 : update history,
		// 5 : price,
		// 6 : update date,
		// 7 : state?
		// }
		// 2 : {}
		// 3 : { 1: active dnd, 2: # ratings, 3: avg rating, 4: ???, 5: total dnd }

		// arrays have changed to objects, with the index as the key
		/*
		 * Per app:
		 * null
		 * [ APP_INFO_ARRAY
		 * * null
		 * * packageName
		 * * Nested array with details
		 * * null
		 * * Nested array with version details
		 * * Nested array with price details
		 * * Last update Date
		 * * Number [1=published, 5 = draft?]
		 * ]
		 * null
		 * [ APP_STATS_ARRAY
		 * * null,
		 * * Active installs
		 * * Total ratings
		 * * Average rating
		 * * Errors
		 * * Total installs
		 * ]
		 */
		JSONObject jsonAppInfo = jsonApp.getJSONObject("1");
		if (DEBUG) {
			pp("jsonAppInfo", jsonAppInfo);
		}
		String packageName = jsonAppInfo.getString("1");
                    
                    
		// TODO figure out the rest and add don't just skip, filter, etc. Cf. #223
		int publishState = jsonAppInfo.optInt("7");
                    
		app.setPublishState(publishState);
		app.setPackageName(packageName);

		// skip if we can't get all the data
		// XXX should we just let this crash so we know there is a problem?
                    if (!jsonAppInfo.has("2")) {
			if (skipIncomplete) {
				if (logger.isDebugEnabled()) {
					logger.debug("(JSONObject jsonApp,boolean skipIncomplete,String developerId,String accountName) - {}", String.format("Skipping app because no app details found: package name=%s", packageName)); //$NON-NLS-1$ //$NON-NLS-2$
					return null;
				}
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("(JSONObject jsonApp,boolean skipIncomplete,String developerId,String accountName) - {}", "Adding incomplete app: " + packageName); //$NON-NLS-1$ //$NON-NLS-2$
				}
	
				
			}
			
		}
                    else{
            			JSONObject appDetails = jsonAppInfo.getJSONObject("2").getJSONArray("1")
        						.getJSONObject(0);
        				if (DEBUG) {
        					pp("appDetails", appDetails);
        				}
        				app.setName(appDetails.getString("2"));
        				String description = appDetails.getString("3");
        				String changelog = appDetails.optString("5");
        				Long lastPlayStoreUpdate = jsonAppInfo.optLong("7");
        				AppDetails details = new AppDetails(description, changelog, lastPlayStoreUpdate);
        				app.setDetails(details);
                    	
                    }
            		
            		JSONArray appVersions =null;
		if (!jsonAppInfo.has("4")) {
			if (skipIncomplete) {
				if (logger.isDebugEnabled()) {
					logger.debug("(JSONObject jsonApp,boolean skipIncomplete,String developerId,String accountName) - {}", String.format("Skipping app because no versions info found: package name=%s", packageName)); //$NON-NLS-1$ //$NON-NLS-2$
					return null;
				}
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("(JSONObject jsonApp,boolean skipIncomplete,String developerId,String accountName) - {}", "Adding incomplete app: " + packageName); //$NON-NLS-1$ //$NON-NLS-2$
				}
				
				
			}
			
		}
		else{
			//Check application is versioned
			appVersions = jsonAppInfo.optJSONObject("4").optJSONArray("1");
		}



		if (DEBUG) {
			pp("appVersions", appVersions);
		}
		if (appVersions == null) {
			if (skipIncomplete) {
				if (logger.isDebugEnabled()) {
					logger.debug("(JSONObject jsonApp,boolean skipIncomplete,String developerId,String accountName) - {}", String.format("Skipping app because no versions info found: package name=%s", packageName)); //$NON-NLS-1$ //$NON-NLS-2$
					return null;
				}
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("(JSONObject jsonApp,boolean skipIncomplete,String developerId,String accountName) - {}", "Adding incomplete app: " + packageName); //$NON-NLS-1$ //$NON-NLS-2$
					
				}
				
				
			}
			
		}
		else
		{
			JSONObject lastAppVersionDetails = appVersions.getJSONObject(appVersions.length() - 1)
					.getJSONObject("2");
			if (DEBUG) {
				pp("lastAppVersionDetails", lastAppVersionDetails);
			}
			app.setVersionName(lastAppVersionDetails.getString("4"));
			app.setIconUrl(lastAppVersionDetails.getJSONObject("6").getString("3"));
		}
                    
		

		// XXX this index might not be correct for all apps?
		// 3 : { 1: active dnd, 2: # ratings, 3: avg rating, 4: #errors?, 5: total dnd }
		JSONObject jsonAppStats = jsonApp.optJSONObject("3");
		if (DEBUG) {
			pp("jsonAppStats", jsonAppStats);
		}
		if (jsonAppStats == null) {
			if (skipIncomplete) {
				if (logger.isDebugEnabled()) {
					logger.debug("(JSONObject jsonApp,boolean skipIncomplete,String developerId,String accountName) - {}", String.format("Skipping app because no stats found: package name=%s", packageName)); //$NON-NLS-1$ //$NON-NLS-2$
					return null;
				}
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("(JSONObject jsonApp,boolean skipIncomplete,String developerId,String accountName) - {}", "Adding incomplete app: " + packageName); //$NON-NLS-1$ //$NON-NLS-2$
				}
				
			}
			
		}
                    
		AppStats stats = new AppStats();
		stats.setRequestDate(now);
		if (jsonAppStats==null||jsonAppStats.length() < 4) {
			// no statistics (yet?) or weird format
			// TODO do we need differentiate?
			stats.setActiveInstalls(0);
			stats.setTotalDownloads(0);
			stats.setNumberOfErrors(0);
                            stats.setAvgRatingDiff(0);
                            
		} else {
                        /*
                         * 1 = Active Installs
                         * 2 = Number Rates
                         * 3 = Average Rating
                         * 4 = Errors
                         * 5 = Total Downloads
                         */
			stats.setActiveInstalls(jsonAppStats.getInt("1"));
			stats.setTotalDownloads(jsonAppStats.getInt("5"));
			stats.setNumberOfComments(jsonAppStats.getInt("2"));
		stats.setNumberOfErrors(jsonAppStats.optInt("4"));
                            stats.setAvgRatingDiff(Float.parseFloat(jsonAppStats.optString("3")));
                            
		}
                    
                    app.setLatestStats(stats);
                                            
		if (logger.isDebugEnabled()) {
			logger.debug("(JSONObject jsonApp,boolean skipIncomplete,String developerId,String accountName) - {}", app.getPackageName() + ", " + app.getAccount() + ", " + app.getName() + ", " + app.getVersionName() + ", " + stats.getTotalDownloads() + ", " + stats.getActiveInstalls() + ", " + stats.getAvgRatingDiff() + ", "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		}
		return app;
	}
	static AppInfo parseAppInfo(String json, String accountName,String developerId, boolean skipIncomplete)
			throws JSONException {

		
		AppInfo app = null;
		// Extract the base array containing apps
		JSONObject result = new JSONObject(json).getJSONObject("result");
		if (DEBUG) {
			pp("result", result);
		}

		JSONArray jsonArray = result.optJSONArray("1");
		
		if (DEBUG) {
			pp("jsonArray", jsonArray);
		}
		if (jsonArray == null) {
			// no apps yet?
			return app;
		}
		JSONObject jsonApp=jsonArray.optJSONObject(0);
		app=parseApp(jsonApp, false,developerId,accountName); 

			return app;
	}

	private static void pp(String name, JSONArray jsonArr) {
		try {
			String pp = jsonArr == null ? "null" : jsonArr.toString(2);
			if (logger.isDebugEnabled()) {
				logger.debug("pp(String, JSONArray) - {}", String.format("%s: %s", name, pp)); //$NON-NLS-1$ //$NON-NLS-2$
				logger.debug("pp(String, JSONArray) - pp on JsonParser: {}", pp); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch (JSONException e) {
			logger.error("pp(String, JSONArray) - {}", "Error printing JSON: " + e.getMessage() + e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static void pp(String name, JSONObject jsonObj) {
		try {
			String pp = jsonObj == null ? "null" : jsonObj.toString(2);
			if (logger.isDebugEnabled()) {
				logger.debug("pp(String, JSONObject) - {}", String.format("%s: %s", name, pp)); //$NON-NLS-1$ //$NON-NLS-2$
				logger.debug("pp(String, JSONObject) - pp on JsonParser: {}", pp); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch (JSONException e) {
			logger.error("pp(String, JSONObject) - {}", "Error printing JSON: " + e.getMessage() + e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Parses the supplied JSON string and returns the number of comments.
	 * 
	 * @param json
	 * @return
	 * @throws JSONException
	 */
	static int parseCommentsCount(String json) throws JSONException {
		// Just extract the number of comments
		/*
		 * null
		 * Array containing arrays of comments
		 * numberOfComments
		 */
		return new JSONObject(json).getJSONObject("result").getInt("2");
	}

	/**
	 * Parses the supplied JSON string and returns a list of comments.
	 * 
	 * @param json
	 * @return
	 * @throws JSONException
	 */
	static List<Comment> parseComments(String json) throws JSONException {
		List<Comment> comments = new ArrayList<Comment>();
		/*
		 * null
		 * Array containing arrays of comments
		 * numberOfComments
		 */
		JSONArray jsonComments = new JSONObject(json).getJSONObject("result").getJSONArray("1");
		int count = jsonComments.length();
		for (int i = 0; i < count; i++) {
			Comment comment = new Comment();
			JSONObject jsonComment = jsonComments.getJSONObject(i);
			// TODO These examples are out of date and need updating
			/*
			 * null
			 * "gaia:17919762185957048423:1:vm:11887109942373535891", -- ID?
			 * "REVIEWERS_NAME",
			 * "1343652956570", -- DATE?
			 * RATING,
			 * null
			 * "COMMENT",
			 * null,
			 * "VERSION_NAME",
			 * [ null,
			 * "DEVICE_CODE_NAME",
			 * "DEVICE_MANFACTURER",
			 * "DEVICE_MODEL"
			 * ],
			 * "LOCALE",
			 * null,
			 * 0
			 */
			// Example with developer reply
			/*
			 * [
			 * null,
			 * "gaia:12824185113034449316:1:vm:18363775304595766012",
			 * "Micka?l",
			 * "1350333837326",
			 * 1,
			 * "",
			 * "Nul\tNul!! N'arrive pas a scanner le moindre code barre!",
			 * 73,
			 * "3.2.5",
			 * [
			 * null,
			 * "X10i",
			 * "SEMC",
			 * "Xperia X10"
			 * ],
			 * "fr_FR",
			 * [
			 * null,
			 * "Prixing fonctionne pourtant bien sur Xperia X10. Essayez de prendre un minimum de recul, au moins 20 ? 30cm, ?vitez les ombres et les reflets. N'h?sitez pas ? nous ?crire sur contact@prixing.fr pour une assistance personnalis?e."
			 * ,
			 * null,
			 * "1350393460968"
			 * ],
			 * 1
			 * ]
			 */
			String uniqueId = jsonComment.getString("1");
			comment.setUniqueId(uniqueId);
			String user = jsonComment.optString("2");
			if (user != null && !"".equals(user) && !"null".equals(user)) {
				comment.setUser(user);
			}
			comment.setDate(parseDate(jsonComment.getLong("3")));
			comment.setRating(jsonComment.getInt("4"));
			String version = jsonComment.optString("7");
			if (version != null && !"".equals(version) && !version.equals("null")) {
				comment.setAppVersion(version);
			}

			String commentLang = jsonComment.optJSONObject("5").getString("1");
			String commentText = jsonComment.optJSONObject("5").getString("3");
			comment.setLanguage(commentLang);
			comment.setOriginalText(commentText);
			// overwritten if translation is available
			comment.setText(commentText);

			JSONObject translation = jsonComment.optJSONObject("11");
			if (translation != null) {
				String displayLanguage = Locale.getDefault().getLanguage();
				String translationLang = translation.getString("1");
				String translationText = translation.getString("3");
				if (translationLang.contains(displayLanguage)) {
					comment.setText(translationText);
				}
			}

			JSONObject jsonDevice = jsonComment.optJSONObject("8");
			if (jsonDevice != null) {
				String device = jsonDevice.optString("3");
				JSONArray extraInfo = jsonDevice.optJSONArray("2");
				if (extraInfo != null) {
					device += " " + extraInfo.optString(0);
				}
				comment.setDevice(device.trim());
			}

			JSONObject jsonReply = jsonComment.optJSONObject("9");
			if (jsonReply != null) {
				Comment reply = new Comment(true);
				reply.setText(jsonReply.getString("1"));
				reply.setDate(parseDate(jsonReply.getLong("3")));
				reply.setOriginalCommentDate(comment.getDate());
				comment.setReply(reply);
			}

			comments.add(comment);
		}

		return comments;
	}

	static Comment parseCommentReplyResponse(String json) throws JSONException {
		// {"result":{"1":{"1":"REPLY","3":"TIME_STAMP"},"2":true},"xsrf":"XSRF_TOKEN"}
		// or
		// {"error":{"data":{"1":ERROR_CODE},"code":ERROR_CODE}}
		JSONObject jsonObj = new JSONObject(json);
		if (jsonObj.has("error")) {
			JSONObject errorObj = jsonObj.getJSONObject("error");
			String data = errorObj.getJSONObject("data").optString("1");
			String errorCode = errorObj.optString("code");
			throw new DevConsoleException(String.format(
					"Error replying to comment: %s, errorCode=%s", data, errorCode));
		}
		JSONObject replyObj = jsonObj.getJSONObject("result").getJSONObject("1");

		Comment result = new Comment(true);
		result.setText(replyObj.getString("1"));
		result.setDate(parseDate(Long.parseLong(replyObj.getString("3"))));

		return result;
	}

	/**
	 * Parses the given date
	 * 
	 * @param unixDateCode
	 * @return
	 */
	private static Date parseDate(long unixDateCode) {
		return new Date(unixDateCode);
	}

}
