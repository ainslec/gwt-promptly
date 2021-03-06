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
package org.ainslec.gwt.promptly.core.client;

import java.util.ArrayList;

import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.FlowPanel;


/**
 * Represents a styled paragraph, which can contain multiple chunks of information.
 * @author Chris Ainsley
 *
 */
public class StyledBlock {
   
   int _length = 0;
   
   ArrayList<StyledParagraphItem> _items = new ArrayList<StyledParagraphItem>();
   
   public StyledBlock() {
      
   }
   
   public StyledBlock append(String text){
      if (text != null) {
         _length+=text.length();
         _items.add(new StyledParagraphItem(null /* style */, text, null /* PromptlyStyledParagraphItem */));
      }
      return this;
   }
   
   public StyledBlock append(String text, String style, ItemListener callback) {
      if (text != null) {
         _length += text.length();
         final StyledParagraphItem item = new StyledParagraphItem(style, text, callback);
         _items.add(item);
      }
      return this;
   }
   
   public void removeItemAt(int itemIndex) {
      int numItems = _items.size();
      if (itemIndex < numItems) {
         StyledParagraphItem itemToRemove = _items.get(itemIndex);
         _length -= itemToRemove.getText().length();
         _items.remove(itemIndex);
      }
   }
   
   SafeHtml toSafeHtml(boolean withFormatting) {

      // NOTE :: Embedded operations are LOST using this method .... 
      
      StringBuilder sb = new StringBuilder();
      
      for (StyledParagraphItem i : _items) {
         final String text = i.getText();
         final String style = i.getStyle();
         
         if (withFormatting && style != null) {
            sb.append("<span style=\"");
            sb.append(SafeHtmlUtils.htmlEscape(style));
            sb.append("\">");
            if (text != null && " ".equals(text)) {
               sb.append("&nbsp;");
            } else {
               sb.append(SafeHtmlUtils.htmlEscape(text));
            }
            sb.append("</span>");
         } else {
            sb.append(SafeHtmlUtils.htmlEscape(text));
         }
      }
      
      final SafeHtml safeHtml = SafeHtmlUtils.fromTrustedString(sb.toString());
      return safeHtml;
   }

   public int length() {
      return _length;
   }
   
   public class Hyperlink implements ClickHandler {
      private long _hyperlinkId;
      private PromptlyPanel promptlyPanel;
      private ItemListener callback;
      private String text;
      
      public Hyperlink(final PromptlyPanel promptlyPanel, long hyperlinkId, ItemListener callback, String text) {
         this.promptlyPanel = promptlyPanel;
         _hyperlinkId= hyperlinkId;
         this.callback = callback;
         this.text = text;
      }
      
      @Override
      public void onClick(ClickEvent event) {
         boolean isBlockedRange = _hyperlinkId < promptlyPanel.getBlockHyperlinkBelowId();
         boolean isAllLinksBlocked = promptlyPanel.isBlockingHyperlinks();
         if ((!isAllLinksBlocked) && (!isBlockedRange)) {
            callback.onClick(promptlyPanel, text, event.getClientX(), event.getClientY());
         }
      }
   }
   
   
   void toGwtWidget(
      final PromptlyPanel promptlyPanel,
      FlowPanel outerWidget,
      boolean withFormatting,
      String outerClassOverride
   ) {

      if (outerClassOverride != null) {
         outerWidget.getElement().setAttribute("class", outerClassOverride);
      }
      
      for (StyledParagraphItem paragraphItem : _items) {
         final String text  = paragraphItem.getText();
         final String style = paragraphItem.getStyle();
         
         FlowPanel spanElement = new FlowPanel(SpanElement.TAG /* <span> tag */ );
         spanElement.getElement().setInnerText(text);
         
         if (withFormatting && style != null) {
            spanElement.getElement().setAttribute("style", style);
         }
         
         final ItemListener callback = paragraphItem.getCallback();
         
         if (callback != null) {
            
            Hyperlink hl = new Hyperlink(promptlyPanel, promptlyPanel.getNextHyperlinkIdAndIncrement(), callback, text);
            spanElement.addDomHandler(hl , ClickEvent.getType());
            spanElement.getElement().setAttribute("onMouseOver", "this.style.fontWeight='normal'");
            spanElement.getElement().setAttribute("onMouseOut",  "this.style.fontWeight='normal'");
         }
         outerWidget.add(spanElement);
      }
   }

   
}
