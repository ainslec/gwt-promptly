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

/**
 * 
 * @author Chris Ainsley
 *
 */
public class AutocompleteCycler {
   String _matchText;
   String[] _sortedMatches;
   int _nextMatchIndex; // -1 represents that matchtext
   
   /**
    * @param text The text that was used to obtain the matches
    * @param sortedMatches Supplied array must have at least 1 item
    */
   public AutocompleteCycler(String text,  String[] sortedMatches) {
      _matchText  = text;
      _sortedMatches = sortedMatches;
      _nextMatchIndex = 0;
   }

   public String next() {
      String retVal;
      if (_nextMatchIndex == -1) {
         retVal = _matchText;
         _nextMatchIndex = 0;
      } else {
         retVal = _sortedMatches[_nextMatchIndex++];
         
         if (_nextMatchIndex == _sortedMatches.length) {
            _nextMatchIndex = -1;
         }
      }
      return retVal;
   }
}