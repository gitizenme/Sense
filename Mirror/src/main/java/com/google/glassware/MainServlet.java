/*
 * Copyright (C) 2013 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.glassware;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.Key;
import com.google.api.services.mirror.model.*;
import com.google.common.collect.Lists;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

/**
 * Handles POST requests from index.jsp
 *
 * @author Jenny Murphy - http://google.com/+JennyMurphy
 */
public class MainServlet extends HttpServlet {

    /**
     * Private class to process batch request results.
     * <p/>
     * For more information, see
     * https://code.google.com/p/google-api-java-client/wiki/Batch.
     */
    private final class BatchCallback extends JsonBatchCallback<TimelineItem> {
        private int success = 0;
        private int failure = 0;

        @Override
        public void onSuccess(TimelineItem item, HttpHeaders headers) throws IOException {
            ++success;
        }

        @Override
        public void onFailure(GoogleJsonError error, HttpHeaders headers) throws IOException {
            ++failure;
            LOG.info("Failed to insert item: " + error.getMessage());
        }
    }

    private static final Logger LOG = Logger.getLogger(MainServlet.class.getSimpleName());
    public static final String CONTACT_ID = "me.izen.sense";
    public static final String CONTACT_NAME = "Sense for Glass";

    private static final String USER_ID = "114337617885938608779";
    private static final int MAX_RESULTS = 3;

    static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private static final String PAGINATED_HTML =
            "<article class='auto-paginate'>"
                    + "<h2 class='blue text-large'>Did you know...?</h2>"
                    + "<p>Cats are <em class='yellow'>solar-powered.</em> The time they spend napping in "
                    + "direct sunlight is necessary to regenerate their internal batteries. Cats that do not "
                    + "receive sufficient charge may exhibit the following symptoms: lethargy, "
                    + "irritability, and disdainful glares. Cats will reactivate on their own automatically "
                    + "after a complete charge cycle; it is recommended that they be left undisturbed during "
                    + "this process to maximize your enjoyment of your cat.</p><br/><p>"
                    + "For more cat maintenance tips, tap to view the website!</p>"
                    + "</article>";


    private static final String API_TOKEN = "c9356fba77923c4cd74c2f797fb1006a";

    public static class PinoccioUrl extends GenericUrl {

        public PinoccioUrl(String encodedUrl) {
            super(encodedUrl);
        }

        /**
         * Maximum number of results.
         */
        @Key
        private int maxResults;

        public int getMaxResults() {
            return maxResults;
        }

        public PinoccioUrl setMaxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        /**
         * Lists the public activities for the given Google+ user ID.
         */
        public static PinoccioUrl getAnalogReport(String queryParams) {
            return new PinoccioUrl(
                    "https://api.pinocc.io/v1/sync?token=" + API_TOKEN + queryParams );
        }
    }

/*

https://api.pinocc.io/v1/stats?token=c9356fba77923c4cd74c2f797fb1006a&scout=1&troop=1&report=analog

&scout=1&troop=1&report=analog

{
    "data": {
        "account":"1432",
        "troop":"1",
        "scout":"1",
        "type":"analog",
        "value": {
            "type":"analog","mode":[-3,-3,2,-3,-3,-3,-3,0],
            "state":[-1,-1,830,-1,-1,-1,-1,749],
            "at":706830,
            "_t":1414288293529
        },
        "time":1414288293529
    }
}
*/


    private static String parseResponse(HttpResponse response) throws IOException {

        String resp = response.parseAsString();

        return resp;
/*
        ActivityFeed feed = response.parseAs(ActivityFeed.class);
        if (feed.getActivities().isEmpty()) {
            System.out.println("No activities found.");
        } else {
            if (feed.getActivities().size() == MAX_RESULTS) {
                System.out.print("First ");
            }
            System.out.println(feed.getActivities().size() + " activities found:");
            for (Activity activity : feed.getActivities()) {
                System.out.println();
                System.out.println("-----------------------------------------------");
                System.out.println("HTML Content: " + activity.getActivityObject().getContent());
                System.out.println("+1's: " + activity.getActivityObject().getPlusOners().getTotalItems());
                System.out.println("URL: " + activity.getUrl());
                System.out.println("ID: " + activity.get("id"));
            }
        }
*/
    }

    /**
     * Do stuff when buttons on index.jsp are clicked
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {

        String userId = AuthUtil.getUserId(req);
        Credential credential = AuthUtil.newAuthorizationCodeFlow().loadCredential(userId);
        String message = "";

        if (req.getParameter("operation").equals("insertSubscription")) {

            // subscribe (only works deployed to production)
            try {
                MirrorClient.insertSubscription(credential, WebUtil.buildUrl(req, "/notify"), userId,
                        req.getParameter("collection"));
                message = "Application is now subscribed to updates.";
            } catch (GoogleJsonResponseException e) {
                LOG.warning("Could not subscribe " + WebUtil.buildUrl(req, "/notify") + " because "
                        + e.getDetails().toPrettyString());
                message = "Failed to subscribe. Check your log for details";
            }

        } else if (req.getParameter("operation").equals("deleteSubscription")) {

            // subscribe (only works deployed to production)
            MirrorClient.deleteSubscription(credential, req.getParameter("subscriptionId"));

            message = "Application has been unsubscribed.";

        } else if (req.getParameter("operation").equals("insertItem")) {
            LOG.fine("Inserting Timeline Item");
            TimelineItem timelineItem = new TimelineItem();

            if (req.getParameter("message") != null) {
                timelineItem.setText(req.getParameter("message"));
            }

            // Triggers an audible tone when the timeline item is received
            timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

            if (req.getParameter("imageUrl") != null) {
                // Attach an image, if we have one
                URL url = new URL(req.getParameter("imageUrl"));
                String contentType = req.getParameter("contentType");
                MirrorClient.insertTimelineItem(credential, timelineItem, contentType, url.openStream());
            } else {
                MirrorClient.insertTimelineItem(credential, timelineItem);
            }

            message = "A timeline item has been inserted.";

        } else if (req.getParameter("operation").equals("insertPaginatedItem")) {
            LOG.fine("Inserting Timeline Item");
            TimelineItem timelineItem = new TimelineItem();
            timelineItem.setHtml(PAGINATED_HTML);

            List<MenuItem> menuItemList = new ArrayList<MenuItem>();
            menuItemList.add(new MenuItem().setAction("OPEN_URI").setPayload(
                    "https://www.google.com/search?q=cat+maintenance+tips"));
            timelineItem.setMenuItems(menuItemList);

            // Triggers an audible tone when the timeline item is received
            timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

            MirrorClient.insertTimelineItem(credential, timelineItem);

            message = "A timeline item has been inserted.";

        } else if (req.getParameter("operation").equals("insertItemWithAction")) {
            LOG.fine("Inserting Timeline Item");
            TimelineItem timelineItem = new TimelineItem();
            timelineItem.setText("Tell me what you had for lunch :)");

            List<MenuItem> menuItemList = new ArrayList<MenuItem>();
            // Built in actions
            menuItemList.add(new MenuItem().setAction("REPLY"));
            menuItemList.add(new MenuItem().setAction("READ_ALOUD"));

            // And custom actions
            List<MenuValue> menuValues = new ArrayList<MenuValue>();
            menuValues.add(new MenuValue().setIconUrl(WebUtil.buildUrl(req, "/static/images/ic_smartphone_black_40dp.png"))
                    .setDisplayName("Drill In"));
            menuItemList.add(new MenuItem().setValues(menuValues).setId("drill").setAction("CUSTOM"));

            timelineItem.setMenuItems(menuItemList);
            timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

            MirrorClient.insertTimelineItem(credential, timelineItem);

            message = "A timeline item with actions has been inserted.";

        } else if (req.getParameter("operation").equals("insertContact")) {
            if (req.getParameter("iconUrl") == null || req.getParameter("name") == null) {
                message = "Must specify iconUrl and name to insert contact";
            } else {
                // Insert a contact
                LOG.fine("Inserting contact Item");
                Contact contact = new Contact();
                contact.setId(req.getParameter("id"));
                contact.setDisplayName(req.getParameter("name"));
                contact.setImageUrls(Lists.newArrayList(req.getParameter("iconUrl")));
                contact.setAcceptCommands(Lists.newArrayList(new Command().setType("TAKE_A_NOTE")));
                MirrorClient.insertContact(credential, contact);

                message = "Inserted contact: " + req.getParameter("name");
            }

        } else if (req.getParameter("operation").equals("deleteContact")) {

            // Insert a contact
            LOG.fine("Deleting contact Item");
            MirrorClient.deleteContact(credential, req.getParameter("id"));

            message = "Contact has been deleted.";

        } else if (req.getParameter("operation").equals("insertItemAllUsers")) {

            // Insert a contact
            List<String> users = AuthUtil.getAllUserIds();
            LOG.info("found " + users.size() + " users");
            if (users.size() > 10) {
                // We wouldn't want you to run out of quota on your first day!
                message =
                        "Total user count is " + users.size() + ". Aborting broadcast " + "to save your quota.";
            } else {
                TimelineItem allUsersItem = new TimelineItem();
                allUsersItem.setText("Hello Everyone!");

                BatchRequest batch = MirrorClient.getMirror(null).batch();
                BatchCallback callback = new BatchCallback();

                for (String user : users) {
                    Credential userCredential = AuthUtil.getCredential(user);
                    MirrorClient.getMirror(userCredential).timeline().insert(allUsersItem)
                            .queue(batch, callback);
                }

                batch.execute();
                message =
                        "Successfully sent cards to " + callback.success + " users (" + callback.failure
                                + " failed).";
            }


        } else if (req.getParameter("operation").equals("deleteTimelineItem")) {

            // Delete a timeline item
            LOG.fine("Deleting Timeline Item");
            MirrorClient.deleteTimelineItem(credential, req.getParameter("itemId"));

            message = "Timeline Item has been deleted.";

        } else if (req.getParameter("operation").equals("queryRemoteSensor")) {

            // Insert a contact
            List<String> users = AuthUtil.getAllUserIds();
            LOG.info("found " + users.size() + " users");
            if (users.size() > 10) {
                // We wouldn't want you to run out of quota on your first day!
                message =
                        "Total user count is " + users.size() + ". Aborting broadcast " + "to save your quota.";
            } else {

                HttpRequestFactory requestFactory =
                        HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
                            @Override
                            public void initialize(HttpRequest request) {
                                request.setParser(new JsonObjectParser(JSON_FACTORY));
                            }
                        });
                PinoccioUrl url = PinoccioUrl.getAnalogReport("&scout=1&troop=1&report=analog&tail=0&start=1414289485000&end=1414289490000").setMaxResults(MAX_RESULTS);
                HttpRequest request = requestFactory.buildGetRequest(url);
                String response = parseResponse(request.execute());


                String dataArray[] = response.split("\n");

                ObjectMapper mapper = new ObjectMapper();


                // map of troop->scout(s)
                HashMap<String, Map<String, String>> troopMap = new HashMap<>();

                for (int i = 0; i < dataArray.length; i++) {
                    LOG.info("data[" + i + "] = " + dataArray[i]);
                    JsonNode rootNode = mapper.readValue(dataArray[i], JsonNode.class);
                    JsonNode type = rootNode.path("data").path("type");
                    if(!type.isMissingNode()) {

                        if(type.getTextValue().equals("connection")) {
                            JsonNode value = rootNode.path("data").path("value");
                            LOG.info("\tconnection = " + value);
// INFO: data[0] = {"data":{"account":"1432","troop":"1","type":"connection","value":{"status":"online","ip":"71.9.231.38"},"time":1414335224550}}
                            String troopKey = rootNode.path("data").path("troop").asText();
                            if(!troopMap.containsKey(troopKey)) {
                                LOG.info("Found new troop id = " + troopKey);
                                HashMap troop = new HashMap<String, String>();
                                troop.put("account", rootNode.path("data").path("account").getTextValue());
                                troop.put("bundleId", Integer.toString(rootNode.path("data").path("time").getIntValue()));
                                troop.put("ip", rootNode.path("data").path("value").path("ip").getTextValue());
                                troop.put("status", rootNode.path("data").path("value").path("status").getTextValue());
                                troopMap.put(troopKey, troop);
                            }
                        }
                        else if(type.getTextValue().equals("scout-name")) {
                            JsonNode value = rootNode.path("data").path("value");
                            LOG.info("\tscout-name = " + value);
// INFO: data[1] = {"data":{"account":"1432","troop":"1","scout":"1","type":"scout-name","value":"Office"}}
                            String troopKey = rootNode.path("data").path("troop").asText();
                            if(troopMap.containsKey(troopKey)) {
                                Map<String, String> troop = troopMap.get(troopKey);
                                troop.put("scout-name", rootNode.path("data").path("value").asText());
                            }
                            else {
                                HashMap troop = new HashMap<String, String>();
                                troop.put("scout-name", rootNode.path("data").path("value").asText());
                                troopMap.put(troopKey, troop);
                            }
                        }
                        else if(type.getTextValue().equals("analog")) {

// INFO: data[2] = {"data":{"account":"1432","troop":"1","scout":"1","type":"analog","value":{"type":"analog","mode":[-3,-3,2,-3,-3,-3,-3,0],"state":[-1,-1,1022,-1,-1,-1,-1,984],"at":36341552,"_t":1414371553764},"time":1414371553764}}
// INFO: data[3] = {"data":{"account":"1432","troop":"1","scout":"2","type":"analog","value":{"type":"analog","mode":[-3,-3,-3,-3,-3,-3,-3,-3],"state":[-1,-1,-1,-1,-1,-1,-1,-1],"at":216,"_t":1414351042018},"time":1414351042018}}

                            JsonNode value = rootNode.path("data").path("value");
                            LOG.info("\tanalog = " + value);
                        }
                        else if(type.getTextValue().equals("power")) {
// INFO: data[14] = {"data":{"account":"1432","troop":"1","scout":"1","type":"power","value":{"type":"power","battery":98,"voltage":414,"charging":false,"vcc":true,"at":61538,"_t":1414335274211},"time":1414335274211}}
// INFO: data[15] = {"data":{"account":"1432","troop":"1","scout":"2","type":"power","value":{"type":"power","battery":100,"voltage":418,"charging":false,"vcc":true,"at":209,"_t":1414351041911.001},"time":1414351041911.001}}
// INFO: 	power = {"type":"power","battery":100,"voltage":418,"charging":false,"vcc":true,"at":209,"_t":1.414351041911001E12}

                            JsonNode power = rootNode.path("data").path("value");
                            LOG.info("\tpower = " + power);

                            Integer battery = power.path("battery").getIntValue();
                            boolean isPowered = power.path("vcc").getBooleanValue();
                            boolean isCharging = power.path("").getBooleanValue();

                            String troopKey = rootNode.path("data").path("troop").asText();
                            String scoutId = "Troop" + troopKey + "Scout" + rootNode.path("data").path("scout").asText();

                            LOG.info("\tscoutId = " + scoutId);

                            Map<String, String> scoutMap;

                            if(troopMap.containsKey(scoutId)) {
                                scoutMap = troopMap.get(scoutId);
                                if(!scoutMap.containsKey("scout-name")) {
                                    scoutMap.put("scout-name", "Scout " + rootNode.path("data").path("scout").asText());
                                }
                            }
                            else {
                                scoutMap = new HashMap<>();
                                troopMap.put(scoutId, scoutMap);
                                scoutMap.put("scout-name", "Scout " + rootNode.path("data").path("scout").asText());
                            }

                            scoutMap.put("battery", battery.toString());
                            scoutMap.put("vcc", isPowered ? "AC" : "BAT");
                            scoutMap.put("charging", isCharging ? "charging" : "not charging");

                        }
                        else if(type.getTextValue().equals("temp")) {
// INFO: data[18] = {"data":{"account":"1432","troop":"1","scout":"1","type":"temp","value":{"type":"temp","c":34,"f":93,"offset":0,"at":35941539,"_t":1414371153538},"time":1414371153538}}
// INFO: data[19] = {"data":{"account":"1432","troop":"1","scout":"2","type":"temp","value":{"type":"temp","c":29,"f":84,"offset":0,"at":20497795,"_t":1414371441246},"time":1414371441246}}
// INFO: 	temp = {"type":"temp","c":29,"f":84,"offset":0,"at":20497795,"_t":1414371441246}

                            JsonNode temp = rootNode.path("data").path("value");
                            LOG.info("\ttemp = " + temp);

                            int tempC = temp.path("c").getIntValue();
                            int tempF = temp.path("f").getIntValue();

                            String troopKey = rootNode.path("data").path("troop").asText();
                            String scoutId = "Troop" + troopKey + "Scout" + rootNode.path("data").path("scout").asText();

                            Map<String, String> scoutMap;

                            if(troopMap.containsKey(scoutId)) {
                                scoutMap = troopMap.get(scoutId);
                                if(!scoutMap.containsKey("scout-name")) {
                                    scoutMap.put("scout-name", "Scout " + rootNode.path("data").path("scout").asText());
                                }
                            }
                            else {
                                scoutMap = new HashMap<>();
                                scoutMap.put("scout-name", "Scout " + rootNode.path("data").path("scout").asText());
                                troopMap.put(scoutId, scoutMap);
                            }

                            scoutMap.put("tempC", tempC + " C");
                            scoutMap.put("tempF", tempF + " F");
                        }
                    }
                    else {
                        LOG.warning("Missing node: type");
                    }
                }

                LOG.info("Total number of scouts: " + troopMap.size());

                Iterator<String> i = troopMap.keySet().iterator();
                while (i.hasNext()) {
                    LOG.info("key = " + i.next());
                }

                for(int troopIdx = 1; troopIdx < 4; troopIdx++)  {
                    if(troopMap.containsKey(Integer.toString(troopIdx))) {
                        LOG.info("Creating  update for troop: " + troopIdx);

                        Map<String, String> troop = troopMap.get(Integer.toString(troopIdx));

                        TimelineItem timelineItem = new TimelineItem();
//                        if(troop.containsKey("bundleId")) {
//                            LOG.info("Setting bundle ID to: " + troop.get("bundleId"));
//                            allUsersItem.setBundleId(troop.get("bundleId"));
//                        }

                        String cardHTML = "<article>";

                        Location location = MirrorClient.getMirror(credential).locations().get("latest").execute();
                        timelineItem.setLocation(location);
                        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));
                        timelineItem.setTitle("Pinoccio Troop Report");


                        // 39.42515,-119.718208
                        if(troopIdx == 1) {
                            cardHTML += "<figure><img src=\"glass://map?w=240&h=360&marker=0;" +
                                    "39.42515,-119.718208;" +
//                                    "marker=1;" +
//                                    "39.42515,-119.718208;" +
                                    "height=\"360\" width=\"240\">" +
                                    "</figure><section>\n" +
                                    "    <div class=\"text-auto-size\"><p class=\"yellow\">Pinoccio Troop Report</p><p>Troop " + troopIdx + "</p></div>" +
                                    "  </section></article>";
                        }
                        else {
                            cardHTML += "<figure><img src=\"glass://map?w=240&h=360&marker=0;" +
                                    location.getLatitude() + "," + location.getLongitude() + ";" +
//                                    "marker=1;" +
//                                    location.getLatitude() + "," + location.getLongitude() +
                                    "height=\"360\" width=\"240\">" +
                                    "</figure><section>\n" +
                                    "    <div class=\"text-auto-size\"><p class=\"yellow\">Pinoccio Troop Report</p><p>Troop " + troopIdx + "</p></div>" +
                                    "  </section></article>";
                        }

                        timelineItem.setText("Pinoccio Troop " + troopIdx);
//                        allUsersItem.setDisplayTime(new DateTime(new Date()));

                        cardHTML += "<article><ul><li>Troop #<p class=\"yellow\">" + troopIdx  + "</p></li>";
                        cardHTML += "<li>Name: <p class=\"yellow\">" + troop.get("scout-name")  + "</p></li>";
                        cardHTML += "<li>Status: <p class=\"yellow\">" + troop.get("status")  + "</p></li></ul></article>";

                        for(int scoutIdx = 1; scoutIdx < 4; scoutIdx++)  {
                            String scoutId = "Troop" + troopIdx + "Scout" + scoutIdx;

                            if(troopMap.containsKey(scoutId)) {
                                LOG.info("\tGenerating content for: scoutId = " + scoutId);
                                Map<String, String> scout = troopMap.get(scoutId);

                                cardHTML += "<article class=\"auto-paginate\"><ul><li>Scout #<p class=\"yellow\">" + scoutIdx  + "</p></li>";
                                cardHTML += "<li>Battery: <p class=\"yellow\">" + scout.get("battery")  + "%</p></li>";
                                cardHTML += "<li>Power: <p class=\"yellow\">" + scout.get("vcc")  + "</p></li>";
                                cardHTML += "<li>Charging: <p class=\"yellow\">" + scout.get("charging")  + "</p></li>";
                                cardHTML += "<li>Temperature: <p class=\"yellow\">" + scout.get("tempF")  + "</p></li>";
                                cardHTML += "</ul></article>";
                            }
                        }


                        LOG.info("Setting cardHTML: " + cardHTML);

                        timelineItem.setHtml(cardHTML);


                        LOG.info("Sending update to: " + userId);
                        MirrorClient.insertTimelineItem(credential, timelineItem);

//                        BatchRequest batch = MirrorClient.getMirror(null).batch();
//                        BatchCallback callback = new BatchCallback();

//                        MirrorClient.getMirror(credential).timeline().insert(allUsersItem)
//                                .queue(batch, callback);


//                        for (String user : users) {
//                            Credential userCredential = AuthUtil.getCredential(user);
//                            Location location = MirrorClient.getMirror(userCredential).locations().get("latest").execute();
//                            allUsersItem.setLocation(location);
//                        }

//                        batch.execute();

                        message = "Successfully sent cards to " + userId;
                    }

                }

            }
        } else {
            String operation = req.getParameter("operation");
            LOG.warning("Unknown operation specified " + operation);
            message = "I don't know how to do that";
        }
        WebUtil.setFlash(req, message);
        res.sendRedirect(WebUtil.buildUrl(req, "/"));
    }
}
