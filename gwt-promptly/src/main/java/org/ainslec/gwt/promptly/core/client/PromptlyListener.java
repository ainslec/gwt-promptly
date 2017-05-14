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


/**
 * A listener for events fired from the command line panel
 * @author Chris Ainsley
 *
 */
public interface PromptlyListener {
   
   /**
    * Fired when not in command line mode and the view is single clicked / touched.
    * @param panel A non-null panel for which this even relates
    * @param clientX The client x position of the click / touch
    * @param clientY The client y position of the click / touch
    */
	public void onClickInNonCommandMode(PromptlyPanel panel, int clientX, int clientY);
	
   /**
    * NOTE: This only supports a subset of keys on the keyboard currently, including all
    * letters and numbers.
    * @param panel A non-null panel
    * @param c The character that was pressed
    */
   public void onKeypressedInNonCommandMode(PromptlyPanel panel, char c);
   
   /**
    * This will only fire when we are in command line mode (prompt is visible).
    * @param panel A non-null panel
    * @param text The content of the command line when the 'enter' button is pressed. 
    */
	public void onTextEnteredInCommandMode(PromptlyPanel panel, String text);
	
	/**
	 * Fired when the tab button is pressed in command line mode. This will not be fired unless
	 * {@link PromptlyPanel#isSupportTabCapture()} returns a true value (this method can be overridden).
	 * @param panel A non-null panel
	 * @param text Non-null text that is on the command line. The position of the caret on the command line is not taken into consideration.
	 */
	public void onTabPressedInCommandMode(PromptlyPanel panel, String text);
	
	/**
	 * Fired when a key is pressed in command line (prompt visible) mode, and the character is not a tab (or ctrl+c)
	 * @param panel A non-null panel
	 */
	public void onNonTabPressedInCommandMode(PromptlyPanel panel/* , char c */);
	
	/**
	 * If the user presses ctrl+c in any mode, the event is registered here. No other events are fired for ctrl+c. 
	 * @param panel A non-null panel
	 */
   public void onControlCPressedInAllModes(PromptlyPanel panel);
   
   /**
    * If the user presses ctrl+any number in any mode, the event is registered here. No other events are fired for ctrl+[0-9].<br/><br/>
    * IMPORTANT :: if {@link PromptlyPanel#isNeverHandleAndAlwaysPropagateZoomHotkeys()} is overrident to return true, then ctrl+0 will 
    * not be handled by this method (will be propogated to browser). This method will only return values is {@link PromptlyPanel#isCaptureCtrlNumerics()} returns
    * a true value.
    * @param panel A non-null panel
    */
   public void onControlNumberPressedInAllModes(PromptlyPanel panel, int numeric);
   
   
   /**
    * Fired when a double click event is fired. These events are only collected and fired if {@link PromptlyPanel#isCaptureDoubleClick()} is
    * overridden to return a 'true' value.
    * @param panel A non-null panel
    * @param clientX The client x position of the initial click / touch (the second click may be a few pixels away)
    * @param clientY The client y position of the initial click / touch (the second click may be a few pixels away)
    */
   public void onMouseOrTouchDoubleClick(PromptlyPanel panel, int clientX, int clientY);
}