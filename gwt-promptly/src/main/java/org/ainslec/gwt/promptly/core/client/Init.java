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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.LinkElement;

/**
 * @author Chris Ainsley
 */
public class Init implements EntryPoint {

	public void onModuleLoad() {
		final String linkTxt = Resources.INSTANCE.linkTxt().getText();
		final Document document = Document.get();
		HeadElement headElement = document.getHead();
		if (headElement == null) {
			headElement = document.createHeadElement();
			document.appendChild(headElement);
		}
		LinkElement linkElement = document.createLinkElement();
		linkElement.setType("text/css");
		linkElement.setRel("stylesheet");
		linkElement.setHref(linkTxt);
		headElement.appendChild(linkElement);
	}

}
