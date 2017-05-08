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
 * Default listener implementation (does nothing)
 * @author Chris Ainsley
 *
 */
public class DefaultPromptlyListener implements PromptlyListener {
   @Override public void onTextEnteredInCommandMode(PromptlyPanel panel, String text) {}
   @Override public void onClickInNonCommandMode(PromptlyPanel panel, int clientX, int clientY) { }
   @Override public void onKeypressedInNonCommandMode(PromptlyPanel panel, char c) { }
   @Override public void onControlCPressedInAllModes(PromptlyPanel panel) { }
   @Override public void onTabPressedInCommandMode(PromptlyPanel panel, String text) { }
   @Override public void onNonTabPressedInCommandMode(PromptlyPanel panel) { }
   @Override public void onMouseOrTouchDoubleClick(PromptlyPanel panel) { }
}
