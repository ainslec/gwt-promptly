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
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.dom.client.UListElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;


/**
 * A console-like abstraction atop of HTML. Allows styled text to be added to a console window,
 * captured prescribed mouse and touch events. Provides a command line at the bottom of the panel. Optionally
 * records history for the command line. Can enter into 'direct input' mode where the textbox at the bottom is not available.
 * </br></br>NOTE: Uses flexboxes, and requires the 'gwtpromptly' and gwtpromptlbackground' styles to be included in the core
 * webpage. This usually happens transparently via the library entry point injecting the stylesheet into the 'head' block
 * of the webpage.
 * @author Chris Ainsley
 *
 */
public class PromptlyPanel extends Composite {

   private static final int ACCEPTABLE_CLICK_EVENT_JUDDER_PIXELS = 6;
   private static final int DOUBLE_CLICK_THRESHOLD_MILLIS        = 450; // Faster than Microsoft recommanded delay of 500 milliseconds
   public static final String DEFAULT_TEXT_STYLE_CLASSNAME   = "gwtpromptly";
   public static final String DEFAULT_BACKGROUND_STYLE_NAME  = "gwtpromptlybackground";
   public static final String DEFAULT_TEXTBOX_STYLE          = "flex-grow:1;font-family:inherit;font-size:inherit;color:inherit; background-color: transparent; border: 0px solid;outline: none;text-shadow:inherit;";
	public static final String DEFAULT_CARET_STYLE_VISIBLE    = "flex-grow:0;padding-right:4px;";
	public static final String DEFAULT_CARET_STYLE_INVISIBLE  = "flex-grow:0;padding-right:4px;display:none;";
	public static final boolean DEFAULT_SUPPORT_TAB_CAPTURE   = true;
	
   private static PromptlyListener DEFAULT_LISTENER = new DefaultPromptlyListener();
   
   private PromptlyListener             _listener           = DEFAULT_LISTENER;
	private boolean                   _isCommandLineMode  = true;
	private boolean                   _collectKeyEventsWhenInNonCommandLineMode   = true;
	private boolean                   _collectMouseEventsWhenInNonCommandLineMode = true;

   private FlowPanel                 _caret;
	private FlowPanel                 _outerPanel;         // Panel that fills all available space
	protected FlowPanel               _mainTextFlowDiv;    // Panel that has max width, but stretches to use all available height
	private FlowPanel                 _commandLineWrapper;
	private TextBox                   _commandLineTextBox;
	  
   private int _commandCacheLimit               = 0;
   private int _commandCacheNextInsertionIndex  = 0;
   private boolean _cacheOverflowed             = false;
   private boolean _blockingHyperlinks;
   
   private int _commandCacheUserCursorIndex     = -1; // The is the next index to be displayed to the user
   private int _commandCacheUserCursorLimit     = -1; // This is the limit of the cursor, so we don't loop around indefinately
   
   private String _stowed = "";
   private String[] _commandCache = new String[]{};
   
	public PromptlyPanel() {
		_outerPanel = new FlowPanel() {
		  //RepeatingCommand _cmd = null;
		  // boolean _flashEnabled = true;
		   
			@Override
	      public void onAttach() {
              super.onAttach();
              sinkEvents(Event.MOUSEEVENTS);
	      }
			
			@Override
			protected void onDetach() {
			   super.onDetach();
//			   if (_cmd != null) {
//			      _flashEnabled = false;
//			      _cmd          = null;
//			   }
			}
			
			int _clientX = -1;
			int _clientY = -1;
			
			Timer _pendingClick = null;
			
         @Override
         public void onBrowserEvent(final Event event) {
            
            int typeInt = event.getTypeInt();

            if ((typeInt & Event.ONMOUSEDOWN) != 0) {
               int buttonId = event.getButton();
               if ((NativeEvent.BUTTON_LEFT & buttonId) != 0) {
                  _clientX = event.getClientX();
                  _clientY = event.getClientY();
               } else {
                  event.preventDefault();
               }
            } else if  ((typeInt & Event.ONMOUSEUP) != 0) {
               
               int buttonId = event.getButton();
               
               if ((NativeEvent.BUTTON_LEFT & buttonId) != 0) {
                     final int x3 = event.getClientX();
                     final int y3 = event.getClientY();

                     // Setting focust to text area steals selection from
                     // a drag event, so we process click events on mouse up
                     // and if the mouse has moved more than ACCEPTABLE_CLICK_EVENT_JUDDER_PIXELS pixels 
                     // in x or y direction between the mouse down and the mouse up event, then
                     // Do not process as a click event, and let the browser deal with the drag
                     // event naturally (text selection)
                     
                     if (!isDragging(x3, y3)) {
                        if (isCaptureDoubleClick() /* Double click handling introduces some latency into the click events, so let's make it optional */) {
                           if (_pendingClick != null) {
                              _pendingClick.cancel();
                              _pendingClick = null;
                              _listener.onMouseOrTouchDoubleClick(PromptlyPanel.this);
                           } else {
                              // We use a timer delay to detect if a click is a single click or double click.
                              _pendingClick = new Timer() {
                                 @Override
                                 public void run() {
                                    handleClickEvent(x3, y3);
                                 }
                              };
                              int doubleClickDetectionThresholdMillis = getDoubleClickDetectionThresholdMillis() < 100 ? 100 : getDoubleClickDetectionThresholdMillis() > 1200 ? 1200 : getDoubleClickDetectionThresholdMillis();
                              _pendingClick.schedule(doubleClickDetectionThresholdMillis);
                           }
                        } else {
                           handleClickEvent(x3, y3);
                        }
                     }
               }
               _clientX = -1; // Always reset these whether right click or not
               _clientY = -1; // Always reset these whether right click or not
            }
         }

         private boolean isDragging(int clientX, int clientY) {
            // Judder protection
            if (_clientX == -1 || (Math.abs(clientX - _clientX) <= ACCEPTABLE_CLICK_EVENT_JUDDER_PIXELS && Math.abs(clientY - _clientY) <= 6)) {
               return false;
            }
            return true;
         }

         private void handleClickEvent(final int x3, final int y3) {
            _pendingClick = null;
            if (_isCommandLineMode) {
               setFocusToTextArea();
            } else {
               _listener.onClickInNonCommandMode(PromptlyPanel.this, x3, y3);
            }
         }
			
		};
		
		String backgroundStyleName = getBackgroundStyleName();

		if (backgroundStyleName != null) {
		   _outerPanel.setStyleName(backgroundStyleName);
		}
		
		_mainTextFlowDiv = new FlowPanel();
		
      String textStyleName = getTextClassName();
      
		if (textStyleName != null) {
		   _mainTextFlowDiv.setStyleName(textStyleName);
		}
		
		if (getTextAdditionalStyles() != null && getTextAdditionalStyles().length() > 0) {
		   _mainTextFlowDiv.getElement().setAttribute("style", getTextAdditionalStyles());
		}
		
		initCommandLineTextEntryTextBox();
		
		_mainTextFlowDiv.add(_commandLineWrapper);
		_outerPanel.add(_mainTextFlowDiv);
		
		initWidget(_outerPanel);
	}


   protected final void initCommandLineTextEntryTextBox() {
		_commandLineWrapper = new FlowPanel();
		_commandLineWrapper.getElement().setAttribute("style", "display: flex;");

		_caret = new FlowPanel();
		
		String caretStyleVisible = getCaretStyleVisible();
		
		setCaretStyleVisible(caretStyleVisible);
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
		
		String textboxStyle = getDefaultCommandLineTextboxStyle();
		
		setCommandLineTextboxStyle(textboxStyle);
		
		final KeyDownHandler keyDownHandler = new KeyDownHandler() {
			
			@Override
			public void onKeyDown(KeyDownEvent event) {
				
			   if (_collectKeyEventsWhenInNonCommandLineMode /* Collection of events can be halted */) {
   			   if (event.getNativeKeyCode() == KeyCodes.KEY_C && event.isControlKeyDown()) {
   			      _listener.onControlCPressedInAllModes(PromptlyPanel.this);
   			      //event.stopPropagation();
   			   } else {
      				if (_isCommandLineMode) {
      				   
                     int nativeKeyCode = event.getNativeKeyCode();
                     if (nativeKeyCode == KeyCodes.KEY_TAB) {
                        boolean isSupportsTabCapture = isSupportTabCapture();
                        if (isSupportsTabCapture) {
                          String innerText = _commandLineTextBox.getText();
                           event.stopPropagation();
                           event.preventDefault();
                           _listener.onTabPressedInCommandMode(PromptlyPanel.this, innerText);
                        } else {
                           // Let the browser deal with the tab event
                        }
                     } else {
         					if (nativeKeyCode == KeyCodes.KEY_ENTER /* carriage return */) {
         						String innerText = _commandLineTextBox.getText();
         						addCacheItem(innerText);
         						_listener.onTextEnteredInCommandMode(PromptlyPanel.this, innerText);
         						boolean clearTextboxOnCarriageReturnPressed = isClearTextboxOnCarriageReturnPressed();
                           if (clearTextboxOnCarriageReturnPressed) {
         							_commandLineTextBox.setText("");
         						}
         						event.stopPropagation();
         					}  else {
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
         					_listener.onNonTabPressedInCommandMode(PromptlyPanel.this);
                     }
      					
      				} else {
      					// event can propogate
      					int nativeKeyCode = event.getNativeKeyCode();
      					char keyCode = 0;
      					
      					// TODO :: Support all keys
      					
      					if (nativeKeyCode >= KeyCodes.KEY_A && nativeKeyCode <= KeyCodes.KEY_Z) {
      						keyCode = (char)(((int)'a') + (nativeKeyCode - KeyCodes.KEY_A));
      					} else if (nativeKeyCode >= KeyCodes.KEY_NUM_ZERO && nativeKeyCode <= KeyCodes.KEY_NUM_NINE) {
      						keyCode = (char)(((int)'0') + (nativeKeyCode - KeyCodes.KEY_NUM_ZERO));
      					} else if (nativeKeyCode == KeyCodes.KEY_ESCAPE) {
      						keyCode = 1;
      					}
      
      					_listener.onKeypressedInNonCommandMode(PromptlyPanel.this, keyCode);
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


   /**
    * Usage: PromptlyPanel.setCaretStyleVisible(PromptlyPanel.DEFAULT_CARET_STYLE_VISIBLE + "color:red;");
    * @param caretStyleVisible
    */
   public void setCaretStyleVisible(String caretStyleVisible) {
      if (caretStyleVisible != null) {
		   _caret.getElement().setAttribute("style", caretStyleVisible);
		}
      _caret.getElement().setClassName(null);
   }

   /**
    * Usage : setCommandLineTextboxStyle(PromptlyPanel.DEFAULT_TEXTBOX_STYLE + "caret-color:#ccc;color:red;");
    * @param textboxStyle Sets the textbox style ... to get default, use {@link #getDefaultCommandLineTextboxStyle()}
    */
   public void setCommandLineTextboxStyle(String textboxStyle) {
      _commandLineTextBox.getElement().removeAttribute("style");
      
      if (textboxStyle != null) {
		   _commandLineTextBox.getElement().setAttribute("style", textboxStyle);
		}
      _commandLineTextBox.getElement().setAttribute("autocomplete","off");
      _commandLineTextBox.getElement().setClassName(null);
   }

	
	public final PromptlyPanel setTextEntryListener(PromptlyListener listener) {
		if (listener == null) {
			_listener = DEFAULT_LISTENER;
		} else {
			_listener = listener;
		}
		
		return this;
	}
	
	public final ItemHandle append (String paragraphText) {
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
	
   public final ItemHandle appendPre (String paragraphText) {
      FlowPanel tag = null;
      if (paragraphText.length() == 0) {
         tag = new FlowPanel(BRElement.TAG);
      } else {
         tag = new FlowPanel(PreElement.TAG);
         if (getPreBlockClassName() != null) {
            tag.getElement().setAttribute("class", getPreBlockClassName());
         }
         tag.getElement().setInnerText(paragraphText);
      }
      appendAndScrollOrFocusAsAppropriate(tag);
      return new ItemHandle(this, tag);
   }
	
	
	public final ItemHandle appendEmbeddedImage(HAlignment imageAlignment, String embeddedImageText, String embeddedImageStyle, String description) {
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
	 * @param styledBlock A non null styled paragraph instance
	 * @param withFormatting If false, then styles will be ignored.
	 */
   public final void append(StyledBlock styledBlock, boolean withFormatting) {
      final FlowPanel gwtWidget = styledBlock.toGwtWidget(this, withFormatting, false /* is pre block */, null /* outer style override */);
      appendAndScrollOrFocusAsAppropriate(gwtWidget);
   }

   public final void appendPre(StyledBlock styledBlock, boolean withFormatting) {
      final FlowPanel gwtWidget = styledBlock.toGwtWidget(this, withFormatting, true /* is pre block */, getPreBlockClassName());
      appendAndScrollOrFocusAsAppropriate(gwtWidget);
   }
   
   private final void appendAndScrollOrFocusAsAppropriate(FlowPanel tag) {
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
   

   public final void appendList(ArrayList<String> choices, boolean ordered, String additionalStyle) {
      if (choices == null || choices.size() == 0) {
         return;
      }
      
      FlowPanel htmlList = new FlowPanel(ordered? OListElement.TAG : UListElement.TAG);
      
      if (additionalStyle != null && additionalStyle.length() > 0) {
         _caret.getElement().setAttribute("style", additionalStyle);
      }
      
      for (String choice : choices) {
         FlowPanel item = new FlowPanel(LIElement.TAG);
         item.getElement().setInnerText(choice);
         htmlList.add(item);
      }
      
      appendAndScrollOrFocusAsAppropriate(htmlList);
   }
	
	public final void clearScreen() {
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
	 * @param collectKeyEventsWhenInNonCommandLineMode If not in command line mode, sets whether keyboard events are collected or ignored.
	 * @param collectMouseEventsWhenInNonCommandLineMode If not in command line mode, sets whether mouse events are collected or ignored (DOES NOT APPLY TO VISIBLE HYPERLINKS)
	 */
	public final void setCommandLineMode(final boolean commandLineMode, final boolean collectKeyEventsWhenInNonCommandLineMode, final boolean collectMouseEventsWhenInNonCommandLineMode /* adv = default to true */) {
		ScheduledCommand cmd = new ScheduledCommand() {
			@Override
			public void execute() {
			   if (!commandLineMode) {
			      if (isCleardownCommandLineTextWhenSwitchingToNonCommandMode()) {
			         setCommandLineText("");
			      }
			   }
			   showCaret(commandLineMode);
			   if (commandLineMode != isCommandLineMode()) {
   				boolean refocusOnCommandLine = (!_isCommandLineMode) && commandLineMode;
   				_isCommandLineMode = commandLineMode;
   				if (refocusOnCommandLine) {
   				   setFocusToTextArea();
   				}
			   }
			   _collectKeyEventsWhenInNonCommandLineMode   = collectKeyEventsWhenInNonCommandLineMode;
			   _collectMouseEventsWhenInNonCommandLineMode = collectMouseEventsWhenInNonCommandLineMode;
			}
		};
		Scheduler.get().scheduleDeferred(cmd);
	}
	
	
	public final void showCaret(boolean showCaret) {
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

	public final void setFocusToTextArea() {
		if (isStealFocusOnUpdate()) {
			_commandLineTextBox.setFocus(true);
		}
		_commandLineWrapper.getElement().scrollIntoView();
	}

   public final void setCommandLineText(String string) {
      _commandLineTextBox.setText(string);
   }
   
   public final void setConsoleStyle(String overrideString) {
      if (overrideString != null) {
         _mainTextFlowDiv.getElement().setAttribute("style", overrideString);
      }
   }
   
   public final void setOutPanelStyle(String overrideString) {
      if (overrideString != null) {
         _outerPanel.getElement().setAttribute("style", overrideString);
      }
   }   
   public final void setConsoleClass(String overrideString) {
      if (overrideString != null) {
         _mainTextFlowDiv.getElement().setAttribute("class", overrideString);
      }
   }
   
   public final void setPromptChar(String string) {
      _caret.getElement().setInnerText(string);
   }
   
   /**
    * Override this to be able to change contents of console without refocusing on the textbox every time
    * @return Return a false value here to be able to change contents of console without refocusing on the textbox every time. If this returns a false value then a call to {@link #setFocusToTextArea()} does nothing. 
    */
   public boolean isStealFocusOnUpdate() {
      return true;
   }

   public boolean isClearTextboxOnCarriageReturnPressed() {
      return true;
   }
   
   public boolean isCollectKeyEvents() {
      return _collectKeyEventsWhenInNonCommandLineMode;
   }
   
   public boolean isCollectMouseEvents() {
      return _collectMouseEventsWhenInNonCommandLineMode;
   }
   
   public boolean isCommandLineMode() {
      return _isCommandLineMode;
   }
   
   public boolean isSupportTabCapture() {
      return DEFAULT_SUPPORT_TAB_CAPTURE;
   }

   public String getBackgroundStyleName() {
      return DEFAULT_BACKGROUND_STYLE_NAME;
   }

   public String getTextClassName() {
      return DEFAULT_TEXT_STYLE_CLASSNAME;
   }
   
   /**
    * 
    * @return if null, not used, otherwise, not a bad place to scale the text 'font-size:1.5em;'
    */
   public String getTextAdditionalStyles() {
      return null;
   }
   
   public String getPreBlockClassName() {
      return DEFAULT_TEXT_STYLE_CLASSNAME;
   }
   
   public String getCaretStyleVisible() {
      return DEFAULT_CARET_STYLE_VISIBLE;
   }
   
   public String getCaretStyleInvisible() {
      return DEFAULT_CARET_STYLE_INVISIBLE;
   }
   
   public String getDefaultCommandLineTextboxStyle() {
      return DEFAULT_TEXTBOX_STYLE;
   }
   
   public boolean isCleardownCommandLineTextWhenSwitchingToNonCommandMode() {
      return true;
   }

   public boolean isInitiallyFocusOnTextBox() {
      return true;
   }
   
   public boolean isStopRightClickPropogation() {
      return true;
   }
   
   public final void addCacheItem(String input) {
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
   
   public void setBlockingHyperlinks(boolean blockingHyperlinks) {
      _blockingHyperlinks = blockingHyperlinks;
   }

   public boolean isBlockingHyperlinks() {
      return _blockingHyperlinks;
   }
   
   public boolean isCaptureDoubleClick() {
      return false;
   }
   
   /**
    * 
    * @return Any value below 100 here will be limited to 100, any value over 1200 will be limited to 1200
    */
   public int getDoubleClickDetectionThresholdMillis() {
      return DOUBLE_CLICK_THRESHOLD_MILLIS;
   }
}
