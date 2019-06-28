/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 package com.google.codeu.servlets;
 import com.google.appengine.api.users.UserService;
 import com.google.appengine.api.users.UserServiceFactory;
 import com.google.codeu.data.Datastore;
 import com.google.codeu.data.Message;
 import com.google.gson.Gson;
 import java.io.IOException;
 import java.util.List;
 import javax.servlet.annotation.WebServlet;
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import org.jsoup.Jsoup;
 import org.jsoup.safety.Whitelist;
 //sentiment imports
 import com.google.cloud.language.v1.Document;
 import com.google.cloud.language.v1.LanguageServiceClient;
 import com.google.cloud.language.v1.Sentiment;
 //libraries to validate the URL for image Styling (part1)
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
//Styled Text Part1 Imports
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;


 /** Handles fetching and saving {@link Message} instances. */
@WebServlet("/messages")
public class MessageServlet extends HttpServlet {

 private Datastore datastore;

 @Override
 public void init() {

   datastore = new Datastore();

 }


 /**
  * Responds with a JSON representation of {@link Message} data for a specific user. Responds with
  * an empty array if the user is not provided.
  */


 @Override
 public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

   response.setContentType("application/json");

   String user = request.getParameter("user");

   if (user == null || user.equals("")) {

     //Request is invalid, return empty array
       response.getWriter().println("[]");

     return;

   }

   List<Message> messages = datastore.getMessages(user);
   Gson gson = new Gson();
   String json = gson.toJson(messages);

   response.getWriter().println(json);


 }

 /** Stores a new {@link Message}. */

 @Override
 public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
   UserService userService = UserServiceFactory.getUserService();
   if (!userService.isUserLoggedIn()) {
     response.sendRedirect("/index.html");
    return;
   }

   String user = userService.getCurrentUser().getEmail();
   String text = Jsoup.clean(request.getParameter("text"), Whitelist.none());



   //sentiment detection code
   Document doc = Document.newBuilder().setContent(text).setType(Document.Type.PLAIN_TEXT).build();
   LanguageServiceClient languageService = LanguageServiceClient.create();
   Sentiment sentiment = languageService.analyzeSentiment(doc).getDocumentSentiment();
   double score = sentiment.getScore();
   languageService.close();

   //printing score
   System.out.println("Score: " + sentiment.getScore());

   //Styled Text Part 1 (MOW)
   Parser parser = Parser.builder().build();
   Node document = parser.parse(text);
   HtmlRenderer renderer = HtmlRenderer.builder().build();
   text = renderer.render(document);  // "<p>This is <em>Sparta</em></p>\n"


   //sending message code //updated message code, including images(part1 ) using regex expression
   String regex = "(https?://\\S+\\.(png|jpg|jpeg|gif|png|svg|mp4))";
   //String regex = "(http)?s?:?(\/\/[^"']*\.(?:png|jpg|jpeg|gif|png|svg|mp4))";
   String replacement = "<img src=\"$1\" />";
   String textWithImagesReplaced = text.replaceAll(regex, replacement);

   Message message = new Message(user, textWithImagesReplaced ,score);

   datastore.storeMessage(message);

   response.sendRedirect("/user-page.html?user=" + user);
 }
}
