/*
 * Copyright 2017, Chris Ainsley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ainslec.gwt.promptly.demo.client;

import java.util.TreeSet;
import org.ainslec.gwt.promptly.core.client.DefaultPromptlyListener;
import org.ainslec.gwt.promptly.core.client.HAlignment;
import org.ainslec.gwt.promptly.core.client.ItemListener;
import org.ainslec.gwt.promptly.core.client.PromptlyListener;
import org.ainslec.gwt.promptly.core.client.PromptlyPanel;
import org.ainslec.gwt.promptly.core.client.StyledBlock;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SplitLayoutPanel;

/**
 * A sample command application to demonstrate use of GWT Promptly Command Line Panels
 * @author Chris Ainsley
 */
public class DemoUI extends Composite {
   
   private static final int    HISTORY_CACHE_LIMIT    = 50;
   private static final String CLEARSCREEN_ADVICE     = "Type 'help' for commands.";
   private static final String ECHO_PREFIX            = "> ";
   private static final String COMMAND_CLEAR          = "clear";
   private static final String COMMAND_TRIANGLE       = "triangle";
   private static final String COMMAND_SQUARE         = "square";
   private static final String COMMAND_CIRCLE         = "circle";
   private static final String COMMAND_STAR           = "star";
   private static final String COMMAND_WAITFORKEY     = "waitkey";
   private static final String COMMAND_PAUSE          = "pause";
   private static final String COMMAND_HELP           = "help";
   private static final String COMMAND_DIR            = "dir";
   private static final String COMMAND_SHIP           = "ship";
   private static final String ADDITIONAL_IMAGE_STYLE = "padding-bottom:8px;";
   
   private static final TreeSet<String> SUPPORTED_COMMANDS = new TreeSet<String>();
   
   static{
      SUPPORTED_COMMANDS.add(COMMAND_CLEAR);
      SUPPORTED_COMMANDS.add(COMMAND_TRIANGLE);
      SUPPORTED_COMMANDS.add(COMMAND_SQUARE);
      SUPPORTED_COMMANDS.add(COMMAND_CIRCLE);
      SUPPORTED_COMMANDS.add(COMMAND_STAR);
      SUPPORTED_COMMANDS.add(COMMAND_SHIP);
      SUPPORTED_COMMANDS.add(COMMAND_WAITFORKEY);
      SUPPORTED_COMMANDS.add(COMMAND_PAUSE);
      SUPPORTED_COMMANDS.add(COMMAND_HELP);
      SUPPORTED_COMMANDS.add(COMMAND_DIR);
   }
   
   //private SplitLayoutPanel   _top;
   private PromptlyPanel      _leftConsole;
   //private PromptlyPanel      _rightConsole;
   private AutocompleteCycler _cycler;
   
   public static final String EMBEDDED_SOURCE__STAR     = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAIAAAAlC+aJAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAG4SURBVGhD7ZjdbcMwDITdrtQOkNcukCGzQF8zQDqTS4CBYegvFHk0Y4DfSwTEtu7Mo2T7Y13X5cx8Pn9PSxqIJg1EkwaiOb2BIzayy88fD+6/XzwA4l6BTT2xH6PIHojG10CdGXiKsgLROBropQWboqxANDEGgCnyMlBIvN++nyM0GaFoXAw0I16kCNUGR1TArwGI00dI+kJjqfhWgcv1wQMFvZeh1waMYS2jb/BA1DZyFZoE3tCiHtClaKBVF6RmG1ibGH5He956TSyNUO98Y1MWzKonpr8LNUsBqYNCPTFtgIDHSSed0axC2DhZ1BPKZRTlwaie0ERoj6Ul7OoJ60bWnEyXJYKuNqWeAOzEs1MytUnddQAGaiQRQu2ALgaO5I0M6J64AAaKiZvZaLY1JEXuFSDprH4bYPE1UCuGe7Aa6AV3cL9Hf823AbgCHGvJbeZj7G2Aj5BEPSM/cgDYQE9Tb5e1ezA9zEkiW0hXnDLGdxWqpegeeAY4GuhpxXpwiZBQ4iBOcpP6ChjVE5BSgCM0q8nuAWaApOjUqE9k9Ab2s1oUMOqrWV/qw3FcRo8hDUSTBmJZln+gQtKxX9KewwAAAABJRU5ErkJggg==";
   public static final String EMBEDDED_SOURCE__SQUARE   = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAIAAAAlC+aJAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAACWSURBVGhD7dohDoRADEDRYc+D4wLYtSiOiMJiuQBu7zMkpDgS5J9u/jNT+5PWTVdrLZl94k3LANrDDYzfI6b27NsQ080VohlAez/i9TfHRJj6JaaLR9weA2gG0AygGUAzgGYAzQCaATQDaAbQDKAZQDOAZgDNAJoBNANoBtAMoBlASx/ghyeaAbR/POJcXCGaAbTkAaWcKFgbbYMTidQAAAAASUVORK5CYII=";
   public static final String EMBEDDED_SOURCE__SHIP     = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsIAAA7CARUoSoAAAAF8SURBVHhe7ZjBbcMwDEXtjtJLO0yBjtR0pAIdpr1kldQEflAjsGxLIiUKn+/yFcAxpBeSTjLfFiZinpC0hAAkLSEASUsIQNISApC0hAAkLSEASQu9AP2fw5dXLBYuP1gUonmvBLoVsN6w8Pg6B8177eCzBYwOu4UvAXLwhocXlFtgo0/PHujouiFmQCmdDi/4nAFrDA8vHD4G53nG6jy3jxeslvd//mKVZn39nTPvS5HzZDcRUEKutD2GFKBJjgD/M8AYegFFLfD89YZVf67v31j9Ey2QQQhA0hICkLSEACQt8VUYSUsIQNISQ/BoCO6xNSBLbpcatBVbO01xBWg+HVIHbfEEUm2Bmk+sl4QYgkgX9KiCIgGWG2otQa0Cavq/Jy5nQMsqcDsEW0nIFrC1gVHLX3BbAUKLKnAtQLCWkCXAYgj1proCWvS/ZRW4b4E7VhKGESBYVFuWgMcNtCh/a6r+EOnFuuxrtz+kAE2GmgEWhAAkLSEASco0/QGWU482MUujPgAAAABJRU5ErkJggg==";
   public static final String EMBEDDED_SOURCE__TRIANGLE = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAIAAAAlC+aJAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAIjSURBVGhD7ZPLdcIwFERTSJbpI9sUn1ZSA5mDB2JfZEuynmwTPOduAL35LHi7PLn6Dvh5/xD+0EcdBwzte2/oNWDcfsA/ROsckBKq3/HPoToHPAilgR/F6RwwFeom8dMg9R3w+fUt8KWfBilyAIqK5ADhgwh1HDC0T27wQYTCBqCiWBggfNasXgPG7QfwwGfNihmAcgLtBR4IH7epywBUv4NnPm5TwADUEuh9B8+ELRoUPwClAR7bokGtA1BIoDHAY2GjtQoegLpJcGKjtWoagCoCXZPgRNhulSIHoOgCOBR2rNf6AWgg0HIBHAqb1itsACpmwbmwb6VWDkC2QL8sOBe2rlTMAJQrBCbC7jVaMwCpAs0KgYlwQI2qByBSoFYVsBKOKdaLDUCYQKEVwFA4rEyvNAAxAlVWA1vhyAK9zAAECJRoBObCwTkdZYCAv4NzKhoAa4HsEBAhHL+oAw0QSHH8ovIDYCqQGgiChEvM61gDBLJcYl6ZAbATyAsHccJVZlQ3AGGdQKirzGhpAIwEkjqBUOFCKVUMQExXEO1CKc0OgIVARlcQLVzrQaUDELABKOBaD0oPwLGA+waggHC5qYoGwHozUMPlpkoMwJmA72aghnDFkfIDYLoxKOOKI3EADgQcNwZlhIvelBkAu11AJRe9aTIATwW8dgGVhOtelRlwTFz3qr8BeHRwXPocsBsu/a/+A0+qc8DeOgfsrScfcLn8AtHgf2iNsKt6AAAAAElFTkSuQmCC";
   public static final String EMBEDDED_SOURCE__CIRCLE   = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAIAAAAlC+aJAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAHqSURBVGhD7ZlLTsMwEEDTXoINd+EeSFwOJO7BXVjAKdKBsVzXnp8dJ55EeQvwolLei8dS5V7meZ72TP+Ay8dvWFHMb09h1Yk+AbI0R5eY9oA2aY7mmMaAvvaRhozqgJXUI7UNFQFG9ZfP77Ci+Hp9DisRe4Y1QLaXpTnUGEuGHrCGeoqcoTYoAYL9cvUUIUNukAI4+77qKVyG0MAGbG8fITO4BjqAtN9APWJvuIb/CcPtAfJxpBgRULKxPWJsyAPKTwyxRyyPfghwZY+UApmkaYQ8cw9w+PoReRPYHXBijwgyIaB8/c6JwvQOuHr9CKd0iEO8u/lBUJvYAYfzg5BihxihXXOZ3n/C8h+38xPJvmmfIzSaM2A0Z8BozoDRXLPLFuGKzwOZHsifIzQaIsD5FGX8BQh3v64oDwD8PegZcDhFnFII2MsURaIwO0KuNkGQuQfsaBNSVekQO9kEWeMhoNyE4Q2lQCYp7QAysMHy6DyAPAlDGsiHlnrEDnhoMNoD9AiNbbDbA8f9pR4RLq67ZzTYA0oAIF++d8kQhlO2B/QAQP0BoS1DPVSqPWAKQNQMRI5RpRGLOlIRABgbFmK3B+oCkPUyqtSRlgCkb0aDOtIeEFlS0uwd6RCQofYsl07pH7Ax+tdp10zTDZyAEWHZ7CWYAAAAAElFTkSuQmCC";

   public DemoUI() {
      //_top = new SplitLayoutPanel();
      
      PromptlyListener listener = new DefaultPromptlyListener(){
         
         @Override
         public void onKeypressedInNonCommandMode(PromptlyPanel panel, char c) {
            
            // This keypress handler is only ever called in non-command line mode. And even then, only when keypress events are not suppressed
            panel.append("Pressed '" + c + "'");
            panel.setCommandLineMode(true /* is command line mode */, true /* is collect events */, true /* is collect mouse events */);
            panel.setBlockingHyperlinks(false);
         }
         
         @Override
         public void onClickInNonCommandMode(PromptlyPanel panel, int clientX, int clientY) {
            // This mouse click handler is only ever called in non-command line mode. And even then, only when keypress events are not suppressed
            panel.append("Clicked mouse [" + clientX + ", " + clientY + "]");
            panel.setCommandLineMode(true /* is command line mode */, true /* is collect events */, true /* is collect mouse events */);
            panel.setBlockingHyperlinks(false);
         }
         
         
         @Override
         public void onNonTabPressedInCommandMode(PromptlyPanel panel) {
            _cycler = null;
         }
         
         @Override
         public void onTabPressedInCommandMode(PromptlyPanel panel, String text) {
            if (_cycler != null) {
               String nextCycle = _cycler.next();
               panel.setCommandLineText(nextCycle);
            } else {
               TreeSet<String> matches = new TreeSet<String>();
               for (String candidate : SUPPORTED_COMMANDS) {
                  if (candidate.startsWith(text)) {
                     matches.add(candidate);
                  }
               }
               
               int numMatches = matches.size();
               
               if (numMatches == 0) {
                  // Do nothing
               } else if (numMatches == 1) {
                  String matchToDisplay = matches.iterator().next();
                  panel.setCommandLineText(matchToDisplay);
               } else {
                  String[] sortedMatchArray = matches.toArray(new String[numMatches]);
                  _cycler = new AutocompleteCycler(text, sortedMatchArray);
                  String nextCycle = _cycler.next();
                  panel.setCommandLineText(nextCycle);
               }
            }
         }
         
         @Override
         public void onTextEnteredInCommandMode(final PromptlyPanel panel, String text) {
            DemoUI.this.onTextEnteredInCommandMode(panel, text);
         }

      };
      
      // We override the font size here (we can also adjust via css style override)
      _leftConsole  = new PromptlyPanel(){public String getTextAdditionalStyles() {return "font-size:1.4em;";}}
         .setTextEntryListener(listener).setCommandCacheLimit(HISTORY_CACHE_LIMIT);
         
      // We override the font size here (we can also adjust via css style override)
//      _rightConsole = new PromptlyPanel(){public String getTextAdditionalStyles() {return "font-size:1.4em;";}}
//         .setTextEntryListener(listener).setCommandCacheLimit(HISTORY_CACHE_LIMIT);
      
      _leftConsole.append(CLEARSCREEN_ADVICE);
//      _rightConsole.append(CLEARSCREEN_ADVICE);
      
//      _top.addWest(_leftConsole, 700);
//      _top.add(_rightConsole);
      
      initWidget(_leftConsole);
   }


   private void echoText(PromptlyPanel panel, String text) {
      panel.append(ECHO_PREFIX + text);
   }

   private void executeCommandPause(final PromptlyPanel panel) {
      panel.setBlockingHyperlinks(true);
      panel.append("Pausing for 3 seconds (all input ignored) ....");
      panel.setCommandLineMode(false /* is command line mode */, false /* is collect key events */, false /* is collect mouse events */);

      Timer t = new Timer() {
         
         @Override
         public void run() {
            panel.append("... and we are back !");
            panel.setBlockingHyperlinks(false);
            panel.setCommandLineMode(true /* is command line mode */, true /* is collect key events */, true /* is collect mouse events */);
         }
      };
      
      t.schedule(3000);
   }
   private void executeCommandWaitForKey(final PromptlyPanel panel) {
      panel.append("Please press a key (prompt is disabled) ....");
      panel.setCommandLineMode(false /* is command line mode */, true /* is collect key events */, true /* is collect mouse events */);
      panel.setBlockingHyperlinks(true);
   }

   private void executeCommandDir(final PromptlyPanel panel) {
      panel.appendPre(" Directory of C:\\javalibs\\gwt-2.7.0\n\n29/01/2015  12:36    <DIR>          .\n29/01/2015  12:36    <DIR>          ..\n29/01/2015  12:35             3,330 about.html\n29/01/2015  12:35             1,168 about.txt\n29/01/2015  12:35            12,444 COPYING\n29/01/2015  12:35            15,678 COPYING.html\n29/01/2015  12:35    <DIR>          doc\n29/01/2015  12:35            70,275 gwt-api-checker.jar\n29/01/2015  12:35           198,100 gwt-codeserver.jar\n29/01/2015  12:35        38,520,843 gwt-dev.jar\n29/01/2015  12:35         1,774,291 gwt-elemental.jar\n29/01/2015  12:35            12,800 gwt-ll.dll\n29/01/2015  12:35             7,100 gwt-module.dtd\n29/01/2015  12:35            47,653 gwt-servlet-deps.jar\n29/01/2015  12:35         9,332,290 gwt-servlet.jar\n29/01/2015  12:35        32,026,261 gwt-user.jar\n29/01/2015  12:35               128 i18nCreator\n29/01/2015  12:35                90 i18nCreator.cmd\n29/01/2015  12:35             1,015 release_notes.html\n29/01/2015  12:35            62,211 requestfactory-apt-src.jar\n29/01/2015  12:35            90,993 requestfactory-apt.jar\n29/01/2015  12:35           501,827 requestfactory-client+src.jar\n29/01/2015  12:35           192,611 requestfactory-client-src.jar\n29/01/2015  12:35           309,382 requestfactory-client.jar\n29/01/2015  12:35         2,153,884 requestfactory-server+src.jar\n29/01/2015  12:35           250,080 requestfactory-server-src.jar\n29/01/2015  12:35         1,903,970 requestfactory-server.jar\n29/01/2015  12:36    <DIR>          samples\n29/01/2015  12:35               130 webAppCreator\n29/01/2015  12:35                92 webAppCreator.cmd\n              28 File(s)     87,601,299 bytes\n               4 Dir(s) 234,121,953,280 bytes free", null /* additional style */);
   }

   private void executeCommandHelp(final PromptlyPanel panel) {
      StyledBlock sp = new StyledBlock();
      ItemListener listener = new ItemListener() {
         @Override
         public void onClick(PromptlyPanel panel, String text, int clientX, int clientY) {
            panel.addCacheItem(text); // Add the clicked text into the entered text buffer
            DemoUI.this.onTextEnteredInCommandMode(panel,text);
         }
      };
      sp.append("Commands available are ");
      sp.append(COMMAND_CIRCLE, "color:#3e3;cursor:pointer;", listener).append(", ");
      sp.append(COMMAND_CLEAR, "background-color:#333;color:white;cursor:pointer;", listener).append(", ");
      sp.append(COMMAND_DIR, "background-color:#444;color:blue;cursor:pointer;", listener).append(", ");
      sp.append(COMMAND_PAUSE, "color:cyan;cursor:pointer;", listener).append(", ");
      sp.append(COMMAND_SQUARE, "color:red;cursor: pointer;", listener).append(", ");
      sp.append(COMMAND_SHIP, "color:orange;cursor: pointer;", listener).append(", ");
      sp.append(COMMAND_STAR, "color:blue;cursor:pointer;", listener).append(", ");
      sp.append(COMMAND_TRIANGLE, "color:yellow;cursor:pointer;", listener).append(" and ");
      sp.append(COMMAND_WAITFORKEY, "color:violet;cursor:pointer;", listener).append(".");
      panel.append(sp, true /* with formatting */);
   }

   public void onTextEnteredInCommandMode(final PromptlyPanel panel, String text) {

      echoText(panel, text);
      
      String lcText = text.toLowerCase();
      
      if (SUPPORTED_COMMANDS.contains(lcText)) {
        // Not using string switch statement for Java 6 demo compatibility
         if (COMMAND_STAR.equals(lcText)) {
            panel.appendEmbeddedImage(HAlignment.LEFT, EMBEDDED_SOURCE__STAR, ADDITIONAL_IMAGE_STYLE, COMMAND_STAR);
         } else if (COMMAND_CIRCLE.equals(lcText)) {
            panel.appendEmbeddedImage(HAlignment.LEFT, EMBEDDED_SOURCE__CIRCLE, ADDITIONAL_IMAGE_STYLE, COMMAND_CIRCLE);
         } else if (COMMAND_SQUARE.equals(lcText)) {
            panel.appendEmbeddedImage(HAlignment.LEFT, EMBEDDED_SOURCE__SQUARE, ADDITIONAL_IMAGE_STYLE, COMMAND_SQUARE);
         } else if (COMMAND_SHIP.equals(lcText)) {
            panel.appendEmbeddedImage(HAlignment.LEFT, EMBEDDED_SOURCE__SHIP, ADDITIONAL_IMAGE_STYLE, COMMAND_SHIP);
         } else if (COMMAND_TRIANGLE.equals(lcText)) {
            panel.appendEmbeddedImage(HAlignment.LEFT, EMBEDDED_SOURCE__TRIANGLE, ADDITIONAL_IMAGE_STYLE, COMMAND_TRIANGLE);
         } else if (COMMAND_CLEAR.equals(lcText)) {
            panel.clearScreen();
            panel.append(CLEARSCREEN_ADVICE);
         } else if (COMMAND_WAITFORKEY.equals(lcText)) {
            executeCommandWaitForKey(panel);
         } else if (COMMAND_PAUSE.equals(lcText)) {
            executeCommandPause(panel);
         } else if (COMMAND_HELP.equals(lcText)) {
            executeCommandHelp(panel);
         } else if (COMMAND_DIR.equals(lcText)) {
            executeCommandDir(panel);
         } else {
            panel.append("I don't know how to do that !");
         }
      } else {
         panel.append("I don't know how to do that !");
      }
   }
}
