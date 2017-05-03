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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.BRElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.LIElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.OListElement;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.dom.client.UListElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;


/**
 * 
 * @author Chris Ainsley
 *
 */
public class PromptlyPanel extends Composite {

   public static final String DEFAULT_TEXT_STYLE_NAME        = "gwtpromptly";
   public static final String DEFAULT_BACKGROUND_STYLE_NAME  = "gwtpromptlybackground";
   public static final String DEFAULT_TEXTBOX_STYLE          = "flex-grow:1;font-family:inherit;font-size:inherit;color:inherit; background-color: transparent; border: 0px solid;outline: none;text-shadow:inherit;";
	public static final String DEFAULT_CARET_STYLE_VISIBLE    = "flex-grow:0;padding-right:4px;";
	public static final String DEFAULT_CARET_STYLE_INVISIBLE  = "flex-grow:0;padding-right:4px;display:none;";

   private static PanelListener DEFAULT_LISTENER = new DefaultPanelListener();
   
   private PanelListener             _listener           = DEFAULT_LISTENER;
	private boolean                   _isCommandLineMode  = true;
	private boolean                   _collectKeyEvents   = true;
	private boolean                   _collectMouseEvents = true;

   private FlowPanel                 _caret;
	private FlowPanel                 _outerPanel;         // Panel that fills all available space
	protected FlowPanel               _mainTextFlowDiv;    // Panel that has max width, but stretches to use all available height
	private FlowPanel                 _commandLineWrapper;
	private TextBox                   _commandLineTextBox;
	  
   private int _commandCacheLimit               = 0;
   private int _commandCacheNextInsertionIndex  = 0;
   private boolean _cacheOverflowed             = false;
   
   private int _commandCacheUserCursorIndex     = -1; // The is the next index to be displayed to the user
   private int _commandCacheUserCursorLimit     = -1; // This is the limit of the cursor, so we don't loop around indefinately
   
   private String _stowed = "";
   private String[] _commandCache = new String[]{};
   
   
	ClickHandler _clickHandler = new ClickHandler() {
		@Override public void onClick(ClickEvent event) {
		   if (event.getNativeButton() != NativeEvent.BUTTON_LEFT) {
		      if (isStopRightClickPropogation()) {
		         event.stopPropagation();
		         event.preventDefault();
		      }
		   } else {
		      if (_collectMouseEvents) {
      			if (_isCommandLineMode) {
      				setFocusToTextArea();
      			} else {
      			   int clientX = event.getClientX();
      			   int clientY = event.getClientY();
      				_listener.onClickWhenInputBoxNotShowing(PromptlyPanel.this, clientX, clientY);
      			}
		      }
		   }
		}
	};

	public PromptlyPanel() {
		_outerPanel = new FlowPanel() {
		   
		   //RepeatingCommand _cmd = null;
		  // boolean _flashEnabled = true;
		   
			@Override
	      public void onAttach() {
              super.onAttach();
              // TODO :: Use mousedown, mouseup, and mousemove events to filter out drag events 
              //         (for selecting text for copy paste purposes)
	 		     super.addDomHandler(_clickHandler, ClickEvent.getType());
//	 		     final Scheduler scheduler = Scheduler.get();
//	 		     _cmd = new RepeatingCommand() {
//               @Override
//               public boolean execute() {
//                  // TODO Toggle flash attributes ..... 
//                  return _flashEnabled;
//               }
//            };
            //scheduler.scheduleFixedPeriod(_cmd, 640 /* milliseconds */);
	      }
			
			@Override
			protected void onDetach() {
			   super.onDetach();
//			   if (_cmd != null) {
//			      _flashEnabled = false;
//			      _cmd          = null;
//			   }
			}
			
		};
		
		String backgroundStyleName = getBackgroundStyleName();

		if (backgroundStyleName != null) {
		   _outerPanel.setStyleName(backgroundStyleName);
		}
		
		_mainTextFlowDiv = new FlowPanel();
		
      String textStyleName = getTextStyleName();
		if (textStyleName != null) {
		   _mainTextFlowDiv.setStyleName(textStyleName);
		}
		
		initCommandLineTextEntryTextBox();
		
		_mainTextFlowDiv.add(_commandLineWrapper);
		_outerPanel.add(_mainTextFlowDiv);
		
		initWidget(_outerPanel);
	}


   protected void initCommandLineTextEntryTextBox() {
		_commandLineWrapper = new FlowPanel();
		_commandLineWrapper.getElement().setAttribute("style", "display: flex;");

		_caret = new FlowPanel();
		
		String caretStyleVisible = getCaretStyleVisible();
		
		if (caretStyleVisible != null) {
		   _caret.getElement().setAttribute("style", caretStyleVisible);
		}
		_caret.getElement().setInnerText(">");
		_commandLineWrapper.add(_caret);

		_commandLineTextBox = new TextBox() {
			@Override
			protected void onAttach() {
				super.onAttach();
				if (isInitiallyFocusOnTextBox()) {
					setFocusToTextArea();
				}
			}
		};
		
		String textboxStyle = getTextboxStyle();
		
		if (textboxStyle != null) {
		   _commandLineTextBox.getElement().setAttribute("style", textboxStyle);
		}
		_commandLineTextBox.getElement().setAttribute("autocomplete","off");
		_commandLineTextBox.getElement().setClassName(null);
		
		final KeyDownHandler keyDownHandler = new KeyDownHandler() {
			
			@Override
			public void onKeyDown(KeyDownEvent event) {
				
			   if (_collectKeyEvents /* Collection of events can be halted */) {
   			   if (event.getNativeKeyCode() == KeyCodes.KEY_C && event.isControlKeyDown()) {
   			      _listener.onControlCPressedInAnyMode(PromptlyPanel.this);
   			      event.stopPropagation();
   			   } else {
      				if (_isCommandLineMode) {
      				   
                     int nativeKeyCode = event.getNativeKeyCode();
      					if (nativeKeyCode == 13 /* carriage return */) {
      						String innerText = _commandLineTextBox.getText();
      						recordInputInCacheIfAppropriate(innerText);
      						_listener.onTextEntered(PromptlyPanel.this, innerText);
      						if (isClearTextboxOnCarriageReturnPressed()) {
      							_commandLineTextBox.setText("");
      						}
      						event.stopPropagation();
      					} else if (nativeKeyCode == KeyCodes.KEY_TAB) {
                       String innerText = _commandLineTextBox.getText();
                        recordInputInCacheIfAppropriate(innerText);
                        _listener.onTabPressed(PromptlyPanel.this, innerText);
                        event.stopPropagation();
      					} else {
      					   if (_commandCacheLimit != 0) {
                           if (nativeKeyCode == KeyCodes.KEY_DOWN) {
      					         String historicalCommandText = accessMoreRecentCommandText();
      					         if (historicalCommandText != null) {
      					            _commandLineTextBox.setText(historicalCommandText);
      					            event.stopPropagation();
      					            event.preventDefault();
      					         }
      					      } else if (nativeKeyCode == KeyCodes.KEY_UP) {
      					         String historicalCommandText = accessOlderCommandText();
      					         
      					         if (historicalCommandText != null) {
                                 _commandLineTextBox.setText(historicalCommandText);
                                 event.stopPropagation();
                                 event.preventDefault();
                              }
      					         
      					      }
      					   }
      					}
      				} else {
      					// event can propogate
      					int nativeKeyCode = event.getNativeKeyCode();
      					char keyCode = 0;
      					
      					// TODO :: Support cursor keys ... 
      					
      					if (nativeKeyCode >= KeyCodes.KEY_A && nativeKeyCode <= KeyCodes.KEY_Z) {
      						keyCode = (char)(((int)'a') + (nativeKeyCode - KeyCodes.KEY_A));
      					} else if (nativeKeyCode >= KeyCodes.KEY_NUM_ZERO && nativeKeyCode <= KeyCodes.KEY_NUM_NINE) {
      						keyCode = (char)(((int)'0') + (nativeKeyCode - KeyCodes.KEY_NUM_ZERO));
      					} else if (nativeKeyCode == KeyCodes.KEY_ESCAPE) {
      						keyCode = 1;
      					}
      
      					_listener.onKeypressWhenInputBoxNotShowing(PromptlyPanel.this, keyCode);
      					event.preventDefault();
      					event.stopPropagation();
      				}
			      }
			   } else {
               event.preventDefault();
               event.stopPropagation();
			   }
			}


		};
		
		_commandLineTextBox.addKeyDownHandler(keyDownHandler);
		_commandLineWrapper.addHandler(keyDownHandler, KeyDownEvent.getType());
		_commandLineWrapper.add(_commandLineTextBox);

	}

	
	public PromptlyPanel setTextEntryListener(PanelListener listener) {
		if (listener == null) {
			_listener = DEFAULT_LISTENER;
		} else {
			_listener = listener;
		}
		
		return this;
	}
	
	public ItemHandle append (String paragraphText) {
	   
	   FlowPanel tag = null;
	   
	   if (paragraphText.length() == 0) {
	      tag = new FlowPanel(BRElement.TAG);
	   } else {
   		tag = new FlowPanel(ParagraphElement.TAG);
   		tag.getElement().setInnerText(paragraphText);
	   }
	   
      appendAndScrollOrFocusAsAppropriate(tag);
      
      return new ItemHandle(this, tag);
	}
	
	
	public ItemHandle appendEmbeddedImage(HAlignment imageAlignment, String embeddedImageText, String embeddedImageStyle, String description) {
	   FlowPanel outer = new FlowPanel(DivElement.TAG);
	   
	   String imageAlignment1 = "text-align:center;";
	   
	   if (imageAlignment == HAlignment.LEFT) {
	      imageAlignment1 = "text-align:left;";
	   } else if (imageAlignment == HAlignment.RIGHT) {
	      imageAlignment1 = "text-align:right;";
	   }
	   
	   outer.getElement().setAttribute("style", "display:block;" + imageAlignment1);
	   FlowPanel inner = new FlowPanel(ImageElement.TAG);
	   inner.getElement().setAttribute("alt",   description);
	   inner.getElement().setAttribute("src",   embeddedImageText);
	   inner.getElement().setAttribute("style", embeddedImageStyle);
	   outer.add(inner);
	   appendAndScrollOrFocusAsAppropriate(outer);
	   
	   return new ItemHandle(this, outer);
	}
	
	
	/**
	 * 
	 * @param styledParagraph A non null styled paragraph instance
	 * @param withFormatting If false, then styles will be ignored.
	 */
   public void append (StyledParagraph styledParagraph, boolean withFormatting) {

      final FlowPanel gwtWidget = styledParagraph.toGwtWidget(withFormatting);
      appendAndScrollOrFocusAsAppropriate(gwtWidget);
   }

   private void appendAndScrollOrFocusAsAppropriate(FlowPanel tag) {
      int numWidgets = _mainTextFlowDiv.getWidgetCount();
      if (numWidgets == 0) {
         _mainTextFlowDiv.add(tag);
      } else {
         _mainTextFlowDiv.insert(tag, _mainTextFlowDiv.getWidgetCount()-1);
      }
	   
      if (_isCommandLineMode) {
         setFocusToTextArea();
      } else {
         tag.getElement().scrollIntoView();
      }
   }

   public void appendList(ArrayList<String> choices, boolean ordered) {
      if (choices == null || choices.size() == 0) {
         return;
      }
      
      FlowPanel htmlList = new FlowPanel(ordered? OListElement.TAG : UListElement.TAG);
      
      for (String choice : choices) {
         FlowPanel item = new FlowPanel(LIElement.TAG);
         item.getElement().setInnerText(choice);
         htmlList.add(item);
      }
      
      appendAndScrollOrFocusAsAppropriate(htmlList);
   }
	
	public void clearScreen() {
		int numWidgets = _mainTextFlowDiv.getWidgetCount();
		if (numWidgets > 0) {
			_mainTextFlowDiv.clear();
			_mainTextFlowDiv.add(_commandLineWrapper);
			setFocusToTextArea();
		}
	}
	
	
	/**
	 * 
	 * @param commandLineMode Sets whether command line (prompt mode) is enabled or disabled.
	 * @param collectKeyEvents If not in command line mode, sets whether keyboard events are collected or ignored.
	 * @param collectMouseEvents If not in command line mode, sets whether mouse events are collected or ignored (DOES NOT APPLY TO VISIBLE HYPERLINKS)
	 */
	public void setCommandLineMode(final boolean commandLineMode, final boolean collectKeyEvents, final boolean collectMouseEvents /* adv = default to true */) {
		ScheduledCommand cmd = new ScheduledCommand() {
			@Override
			public void execute() {
			   showCaret(commandLineMode);
			   if (commandLineMode != isCommandLineMode()) {
   				boolean refocusOnCommandLine = (!_isCommandLineMode) && commandLineMode;
   				_isCommandLineMode = commandLineMode;
   				if (refocusOnCommandLine) {
   				   setFocusToTextArea();
   				}
			   }
			   
			   _collectKeyEvents   = collectKeyEvents;
			   _collectMouseEvents = collectMouseEvents;
			}
		};
		Scheduler.get().scheduleDeferred(cmd);
	}
	
	
	public void showCaret(boolean showCaret) {
	   if (showCaret) {
	        String caretStyleVisible = getCaretStyleVisible();
	         if (caretStyleVisible != null) {
	            _caret.getElement().setAttribute("style", caretStyleVisible);
	         } else {
	            _caret.getElement().removeAttribute("style");
	         }
	   } else {
         String caretStyleInvisible = getCaretStyleInvisible();
         if (caretStyleInvisible != null) {
            _caret.getElement().setAttribute("style", caretStyleInvisible);
         } else {
            _caret.getElement().removeAttribute("style");
         }
	   }
	}

	public void setFocusToTextArea() {
		if (isStealFocusOnUpdate()) {
			_commandLineTextBox.setFocus(true);
		}
		_commandLineWrapper.getElement().scrollIntoView();
	}

   public void setCommandLineText(String string) {
      _commandLineTextBox.setText(string);
   }
   
   public void setConsoleStyleOverride(String overrideString) {
      _mainTextFlowDiv.getElement().setAttribute("style", overrideString);
   }
   
   public void setPromptChar(String string) {
      _caret.getElement().setInnerText(string);
   }
   
   /**
    * Override this to be able to change contents of console without refocusing on the textbox every time
    * @return Return a false value here to be able to change contents of console without refocusing on the textbox every time. If this returns a false value then a call to {@link #setFocusToTextArea()} does nothing. 
    */
   public boolean isStealFocusOnUpdate() {
      return true;
   }

   private boolean isClearTextboxOnCarriageReturnPressed() {
      return true;
   }
   
   public boolean isCollectKeyEvents() {
      return _collectKeyEvents;
   }
   
   public boolean isCollectMouseEvents() {
      return _collectMouseEvents;
   }
   
   public boolean isCommandLineMode() {
      return _isCommandLineMode;
   }

   public String getBackgroundStyleName() {
      return DEFAULT_BACKGROUND_STYLE_NAME;
   }

   public String getTextStyleName() {
      return DEFAULT_TEXT_STYLE_NAME;
   }
   
   public String getCaretStyleVisible() {
      return DEFAULT_CARET_STYLE_VISIBLE;
   }
   
   public String getCaretStyleInvisible() {
      return DEFAULT_CARET_STYLE_INVISIBLE;
   }
   
   public String getTextboxStyle() {
      return DEFAULT_TEXTBOX_STYLE;
   }

   public boolean isInitiallyFocusOnTextBox() {
      return true;
   }
   
   public boolean isStopRightClickPropogation() {
      return true;
   }
   
   private void recordInputInCacheIfAppropriate(String input) {
      if (_commandCacheLimit > 0) {
         _stowed                      = "";
         _commandCacheUserCursorIndex = -1; // The cursor is nowhere ... 
         _commandCacheUserCursorLimit = _commandCacheNextInsertionIndex; // This is the limit of the cursor, so we don't loop around indefinately 
         _commandCache[_commandCacheNextInsertionIndex++] = input;
         if (_commandCacheNextInsertionIndex == _commandCacheLimit) {
            _commandCacheNextInsertionIndex = 0;
            _cacheOverflowed = true;
         }
      }
   }
   
   private String accessOlderCommandText() {
      String retVal = null;
      
      // All good now I think !!!
      
      if (_commandCacheLimit > 0 && _commandCacheUserCursorLimit != -1) {
         boolean isFirst = _commandCacheUserCursorIndex == -1;
         
         if (isFirst) {
            _stowed = _commandLineTextBox.getText();
         }
         int interestingIndex = isFirst ? _commandCacheUserCursorLimit : _commandCacheUserCursorIndex - 1 ;
         
         if (interestingIndex < 0) {
            if (_cacheOverflowed) {
               interestingIndex = _commandCacheLimit - 1;   
            } else {
               interestingIndex = _commandCacheUserCursorIndex;
            }
  
         }
         
         if (interestingIndex == _commandCacheUserCursorLimit) {
            interestingIndex = isFirst ? _commandCacheUserCursorLimit : _commandCacheUserCursorIndex;
         }
         
         retVal = _commandCache[interestingIndex];
         _commandCacheUserCursorIndex = interestingIndex;
      }
      
      return retVal;
   }

   private String accessMoreRecentCommandText() {
      String retVal = null;
      
      if (_commandCacheLimit > 0  && _commandCacheUserCursorLimit != -1 && _commandCacheUserCursorIndex != -1) {
         
         int interestingIndex = _commandCacheUserCursorIndex+1;
         
         if (interestingIndex == _commandCacheLimit) {
            if (_cacheOverflowed) {
               interestingIndex = 0;
            } else {
               interestingIndex = _commandCacheUserCursorIndex;
            }
         }
         
         // Good logic ?? ... 
         if (_commandCacheUserCursorIndex == _commandCacheUserCursorLimit) {
            interestingIndex = _commandCacheUserCursorLimit;
            retVal = _stowed;
            _commandCacheUserCursorIndex = -1;
         } else {
            retVal = _commandCache[interestingIndex];
            _commandCacheUserCursorIndex = interestingIndex;
            
         }
         
      }
      
      return retVal;
   }
   
   /**
    * IMPORTANT: Does not currently support resizing more than once
    * @param limit
    * @return
    */
   public PromptlyPanel setCommandCacheLimit(int limit) {
      int oldLimit = _commandCacheLimit;
      _commandCacheLimit = limit < 0 ? 0 : limit > 5000 ? 5000 : limit;
      if (_commandCacheLimit < oldLimit) {
         if (_commandCacheLimit == 0) {
            _commandCache = new String[] {};
            _cacheOverflowed = false;
            _commandCacheUserCursorIndex    = -1;
            _commandCacheUserCursorLimit    = -1;
            _commandCacheNextInsertionIndex = -1;
         } else {
            _commandCacheLimit = oldLimit; // REMOVE THIS WHEN WE IMPLEMENT
            throw new RuntimeException("Restructuring cache not supported as yet");
         }
      } else if (_commandCacheLimit > oldLimit ){
         if (oldLimit == 0) {
            _commandCache = new String[_commandCacheLimit];
            _commandCacheUserCursorIndex    = -1;
            _commandCacheUserCursorLimit    = -1;
            _commandCacheNextInsertionIndex = 0;
         } else {
            _commandCacheLimit = oldLimit; // REMOVE THIS WHEN WE IMPLEMENT
            throw new RuntimeException("Restructuring cache not supported as yet");
         }
         _cacheOverflowed = false;
      } else {
         // Nothing to do if we are not changing the size
      }
      return this;
   }
   
   public int getCommandCacheLimit() {
      return _commandCacheLimit;
   }

   public String getPromptChar() {
      String promptChar = _caret.getElement().getInnerText();
      return promptChar;
   }
   
}
