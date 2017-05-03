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

import org.ainslec.gwt.promptly.core.client.DefaultPanelListener;
import org.ainslec.gwt.promptly.core.client.HAlignment;
import org.ainslec.gwt.promptly.core.client.ItemListener;
import org.ainslec.gwt.promptly.core.client.PanelListener;
import org.ainslec.gwt.promptly.core.client.PromptlyPanel;
import org.ainslec.gwt.promptly.core.client.StyledParagraph;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SplitLayoutPanel;


/**
 * 
 * @author Chris Ainsley
 *
 */
public class DemoUI extends Composite {
   
   
   private static final String ECHO_PREFIX          = "> ";
   private static final String COMMAND_CLEAR        = "clear";
   private static final String COMMAND_TRIANGLE     = "triangle";
   private static final String COMMAND_SQUARE       = "square";
   private static final String COMMAND_CIRCLE       = "circle";
   private static final String COMMAND_STAR         = "star";
   private static final String COMMAND_WAITFORKEY   = "waitkey";
   private static final String COMMAND_PAUSE        = "pause";
   private static final String COMMAND_HELP         = "help";
   private static final String ADDITIONAL_IMAGE_STYLE = "padding-bottom:8px;";
   
   private SplitLayoutPanel _top;
   private PromptlyPanel _leftConsole;
   private PromptlyPanel _rightConsole;

   public static final String EMBEDDED_SOURCE__STAR     = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAIAAAAlC+aJAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAG4SURBVGhD7ZjdbcMwDITdrtQOkNcukCGzQF8zQDqTS4CBYegvFHk0Y4DfSwTEtu7Mo2T7Y13X5cx8Pn9PSxqIJg1EkwaiOb2BIzayy88fD+6/XzwA4l6BTT2xH6PIHojG10CdGXiKsgLROBropQWboqxANDEGgCnyMlBIvN++nyM0GaFoXAw0I16kCNUGR1TArwGI00dI+kJjqfhWgcv1wQMFvZeh1waMYS2jb/BA1DZyFZoE3tCiHtClaKBVF6RmG1ibGH5He956TSyNUO98Y1MWzKonpr8LNUsBqYNCPTFtgIDHSSed0axC2DhZ1BPKZRTlwaie0ERoj6Ul7OoJ60bWnEyXJYKuNqWeAOzEs1MytUnddQAGaiQRQu2ALgaO5I0M6J64AAaKiZvZaLY1JEXuFSDprH4bYPE1UCuGe7Aa6AV3cL9Hf823AbgCHGvJbeZj7G2Aj5BEPSM/cgDYQE9Tb5e1ezA9zEkiW0hXnDLGdxWqpegeeAY4GuhpxXpwiZBQ4iBOcpP6ChjVE5BSgCM0q8nuAWaApOjUqE9k9Ab2s1oUMOqrWV/qw3FcRo8hDUSTBmJZln+gQtKxX9KewwAAAABJRU5ErkJggg==";
   public static final String EMBEDDED_SOURCE__SQUARE   = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAIAAAAlC+aJAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAACWSURBVGhD7dohDoRADEDRYc+D4wLYtSiOiMJiuQBu7zMkpDgS5J9u/jNT+5PWTVdrLZl94k3LANrDDYzfI6b27NsQ080VohlAez/i9TfHRJj6JaaLR9weA2gG0AygGUAzgGYAzQCaATQDaAbQDKAZQDOAZgDNAJoBNANoBtAMoBlASx/ghyeaAbR/POJcXCGaAbTkAaWcKFgbbYMTidQAAAAASUVORK5CYII=";
   public static final String EMBEDDED_SOURCE__TRIANGLE = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAIAAAAlC+aJAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAIjSURBVGhD7ZPLdcIwFERTSJbpI9sUn1ZSA5mDB2JfZEuynmwTPOduAL35LHi7PLn6Dvh5/xD+0EcdBwzte2/oNWDcfsA/ROsckBKq3/HPoToHPAilgR/F6RwwFeom8dMg9R3w+fUt8KWfBilyAIqK5ADhgwh1HDC0T27wQYTCBqCiWBggfNasXgPG7QfwwGfNihmAcgLtBR4IH7epywBUv4NnPm5TwADUEuh9B8+ELRoUPwClAR7bokGtA1BIoDHAY2GjtQoegLpJcGKjtWoagCoCXZPgRNhulSIHoOgCOBR2rNf6AWgg0HIBHAqb1itsACpmwbmwb6VWDkC2QL8sOBe2rlTMAJQrBCbC7jVaMwCpAs0KgYlwQI2qByBSoFYVsBKOKdaLDUCYQKEVwFA4rEyvNAAxAlVWA1vhyAK9zAAECJRoBObCwTkdZYCAv4NzKhoAa4HsEBAhHL+oAw0QSHH8ovIDYCqQGgiChEvM61gDBLJcYl6ZAbATyAsHccJVZlQ3AGGdQKirzGhpAIwEkjqBUOFCKVUMQExXEO1CKc0OgIVARlcQLVzrQaUDELABKOBaD0oPwLGA+waggHC5qYoGwHozUMPlpkoMwJmA72aghnDFkfIDYLoxKOOKI3EADgQcNwZlhIvelBkAu11AJRe9aTIATwW8dgGVhOtelRlwTFz3qr8BeHRwXPocsBsu/a/+A0+qc8DeOgfsrScfcLn8AtHgf2iNsKt6AAAAAElFTkSuQmCC";
   public static final String EMBEDDED_SOURCE__CIRCLE   = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAIAAAAlC+aJAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAHqSURBVGhD7ZlLTsMwEEDTXoINd+EeSFwOJO7BXVjAKdKBsVzXnp8dJ55EeQvwolLei8dS5V7meZ72TP+Ay8dvWFHMb09h1Yk+AbI0R5eY9oA2aY7mmMaAvvaRhozqgJXUI7UNFQFG9ZfP77Ci+Hp9DisRe4Y1QLaXpTnUGEuGHrCGeoqcoTYoAYL9cvUUIUNukAI4+77qKVyG0MAGbG8fITO4BjqAtN9APWJvuIb/CcPtAfJxpBgRULKxPWJsyAPKTwyxRyyPfghwZY+UApmkaYQ8cw9w+PoReRPYHXBijwgyIaB8/c6JwvQOuHr9CKd0iEO8u/lBUJvYAYfzg5BihxihXXOZ3n/C8h+38xPJvmmfIzSaM2A0Z8BozoDRXLPLFuGKzwOZHsifIzQaIsD5FGX8BQh3v64oDwD8PegZcDhFnFII2MsURaIwO0KuNkGQuQfsaBNSVekQO9kEWeMhoNyE4Q2lQCYp7QAysMHy6DyAPAlDGsiHlnrEDnhoMNoD9AiNbbDbA8f9pR4RLq67ZzTYA0oAIF++d8kQhlO2B/QAQP0BoS1DPVSqPWAKQNQMRI5RpRGLOlIRABgbFmK3B+oCkPUyqtSRlgCkb0aDOtIeEFlS0uwd6RCQofYsl07pH7Ax+tdp10zTDZyAEWHZ7CWYAAAAAElFTkSuQmCC";

   
   public DemoUI() {
      _top = new SplitLayoutPanel();
      
      PanelListener listener = new DefaultPanelListener(){
         
         @Override
         public void onKeypressWhenInputBoxNotShowing(PromptlyPanel panel, char c) {
            
            // This keypress handler is only ever called in non-command line mode. And even then, only when keypress events are not suppressed
            panel.append("Pressed '" + c + "'");
            panel.setCommandLineMode(true /* is command line mode */, true /* is collect events */, true /* is collect mouse events */);
         }
         
         @Override
         public void onClickWhenInputBoxNotShowing(PromptlyPanel panel, int clientX, int clientY) {
            // This mouse click handler is only ever called in non-command line mode. And even then, only when keypress events are not suppressed
            panel.append("Clicked mouse [" + clientX + ", " + clientY + "]");
            panel.setCommandLineMode(true /* is command line mode */, true /* is collect events */, true /* is collect mouse events */);
         }
         
         @Override
         public void onTextEntered(final PromptlyPanel panel, String text) {
            
            // Not using string switch statement for Java 6 demo compatibility
            
            echoText(panel, text);
            
            String lcText = text.toLowerCase();
            
            if (COMMAND_STAR.equals(lcText)) {
               displayStar(panel);
            } else if (COMMAND_CIRCLE.equals(lcText)) {
               displayCircle(panel);
            } else if (COMMAND_SQUARE.equals(lcText)) {
               displaySquare(panel);
            } else if (COMMAND_TRIANGLE.equals(lcText)) {
               displayTriangle(panel);
            } else if (COMMAND_CLEAR.equals(lcText)) {
               executCommandClear(panel);
            } else if (COMMAND_WAITFORKEY.equals(lcText)) {
               executeCommandWaitForKey(panel);
            } else if (COMMAND_PAUSE.equals(lcText)) {
               executeCommandPause(panel);
            } else if (COMMAND_HELP.equals(lcText)) {
               executeCommandHelp(panel);
            } else {
               panel.append("I don't know how to do that !");
            }
         }

      };
      
      // The reason we have two panels with identical behaviour is to 
      // demonstrate 
      
      _leftConsole  = new PromptlyPanel().setTextEntryListener(listener).setCommandCacheLimit(3);
      _rightConsole = new PromptlyPanel().setTextEntryListener(listener).setCommandCacheLimit(6);
      
      
      _leftConsole.append("Type 'help' for commands.");
      _rightConsole.append("Type 'help' for commands.");
      
      _top.addWest(_leftConsole, 700);
      _top.add(_rightConsole);
      
      initWidget(_top);
      
   }


   private void displayStar(PromptlyPanel panel) {
      panel.appendEmbeddedImage(HAlignment.LEFT, EMBEDDED_SOURCE__STAR, ADDITIONAL_IMAGE_STYLE, COMMAND_STAR);
   }


   private void displayCircle(PromptlyPanel panel) {
      panel.appendEmbeddedImage(HAlignment.LEFT, EMBEDDED_SOURCE__CIRCLE, ADDITIONAL_IMAGE_STYLE, COMMAND_CIRCLE);
   }


   private void displaySquare(PromptlyPanel panel) {
      panel.appendEmbeddedImage(HAlignment.LEFT, EMBEDDED_SOURCE__SQUARE, ADDITIONAL_IMAGE_STYLE, COMMAND_SQUARE);
   }


   private void displayTriangle(PromptlyPanel panel) {
      panel.appendEmbeddedImage(HAlignment.LEFT, EMBEDDED_SOURCE__TRIANGLE, ADDITIONAL_IMAGE_STYLE, COMMAND_TRIANGLE);
   }


   private void echoText(PromptlyPanel panel, String text) {
      panel.append(ECHO_PREFIX + text);
   }

   private void executeCommandPause(final PromptlyPanel panel) {
      panel.append("Pausing for 3 seconds (all input ignored) ....");
      panel.setCommandLineMode(false /* is command line mode */, false /* is collect key events */, false /* is collect mouse events */);

      Timer t = new Timer() {
         
         @Override
         public void run() {
            panel.append("... and we are back !");
            panel.setCommandLineMode(true /* is command line mode */, true /* is collect key events */, true /* is collect mouse events */);
         }
      };
      
      t.schedule(3000);
   }
   private void executeCommandWaitForKey(final PromptlyPanel panel) {
      panel.append("Please press a key (prompt is disabled) ....");
      panel.setCommandLineMode(false /* is command line mode */, true /* is collect key events */, true /* is collect mouse events */);
   }


   private void executCommandClear(final PromptlyPanel panel) {
      panel.clearScreen();
   }


   private void executeCommandHelp(final PromptlyPanel panel) {
      StyledParagraph sp = new StyledParagraph();
      sp.append("Commands available are ");
      sp.append(COMMAND_SQUARE, "color:red;cursor: pointer;", new ItemListener() {
         @Override
         public void onClick(int clientX, int clientY) {
            echoText(panel, COMMAND_SQUARE);
            displaySquare(panel);
         }
      });
      sp.append(", ");
      
      sp.append(COMMAND_CIRCLE, "color:#3e3;cursor:pointer;", new ItemListener() {
         @Override
         public void onClick(int clientX, int clientY) {
            echoText(panel, COMMAND_CIRCLE);
            displayCircle(panel);
         }
      });
      sp.append(", ");
      
      sp.append(COMMAND_TRIANGLE, "color:yellow;cursor:pointer;", new ItemListener() {
         @Override
         public void onClick(int clientX, int clientY) {
            echoText(panel, COMMAND_TRIANGLE);
            displayTriangle(panel);
         }
      });
      sp.append(", ");
      
      sp.append(COMMAND_STAR, "color:blue;cursor:pointer;", new ItemListener() {
         @Override
         public void onClick(int clientX, int clientY) {
            echoText(panel, COMMAND_STAR);
            displayStar(panel);
         }
      });
      sp.append(", ");
      
      sp.append(COMMAND_WAITFORKEY, "color:violet;cursor:pointer;", new ItemListener() {
         @Override
         public void onClick(int clientX, int clientY) {
            echoText(panel, COMMAND_WAITFORKEY);
            executeCommandWaitForKey(panel);
         }
      });
      
      sp.append(", ");
      
      sp.append(COMMAND_PAUSE, "color:cyan;cursor:pointer;", new ItemListener() {
         @Override
         public void onClick(int clientX, int clientY) {
            echoText(panel, COMMAND_PAUSE);
            executeCommandPause(panel);
         }
      });
      
      sp.append(" and ");
      sp.append("clear", "background-color:blue;color:white;cursor:pointer;", new ItemListener() {
         @Override
         public void onClick(int clientX, int clientY) {
            executCommandClear(panel);
         }
      });
      sp.append(".");
      panel.append(sp, true /* with formatting */);
   }
}
