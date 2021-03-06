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
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.UListElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
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

   private static final int    ACCEPTABLE_CLICK_EVENT_JUDDER_PIXELS = 6;
   private static final int    DOUBLE_CLICK_THRESHOLD_MILLIS        = 450; // Faster than Microsoft recommanded delay of 500 milliseconds
   public static final String  DEFAULT_TEXT_STYLE_CLASSNAME         = "gwtpromptly";
   public static final String  DEFAULT_BACKGROUND_STYLE_NAME        = "gwtpromptlybackground";
   public static final String  DEFAULT_TEXTBOX_STYLE                = "flex-grow:1;font-family:inherit;font-size:inherit;color:inherit; background-color: transparent; border: 0px solid;outline: none;text-shadow:inherit;";
	public static final String  DEFAULT_PROMPT_STYLE_VISIBLE         = "align-self:center;flex-grow:0;padding-right:4px;";
	public static final String  DEFAULT_PROMPT_STYLE_INVISIBLE       = "align-self:center;flex-grow:0;padding-right:4px;display:none;";
	public static final boolean DEFAULT_SUPPORT_TAB_CAPTURE         = true;
	
   private static PromptlyListener DEFAULT_LISTENER = new DefaultPromptlyListener();
   
   private PromptlyListener          _listener           = DEFAULT_LISTENER;
	private boolean                   _isCommandLineMode  = true;
	private boolean                   _collectKeyEventsWhenInNonCommandLineMode   = true;
	private boolean                   _collectMouseEventsWhenInNonCommandLineMode = true;
   
	private boolean                    _isTurnCaretIntoBackgroundColorWhenNotInCommandMode = true;
	
	private FlowPanel                 _promptZone;
	private FlowPanel                 _promptZone1;
	private FlowPanel                 _promptZone2;
   private FlowPanel                 _promptChar;
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
   private FlowPanel _zone2Image;
   
   private boolean _blockingHyperlinks;
   private long _nextHyperlinkId        = 0;
   private long _blockHyperlinkBelowId  = 0;
   
   public long getNextHyperlinkIdAndIncrement() {
      return _nextHyperlinkId++;
   }
   
   public long getNextHyperlinkId() {
      return _nextHyperlinkId;
   }
   
   public void setBlockHyperlinkBelowId(long blockHyperlinkBelowId) {
      _blockHyperlinkBelowId = blockHyperlinkBelowId;
   }
   
   public long getBlockHyperlinkBelowId() {
      return _blockHyperlinkBelowId;
   }
   
   public void neutralizeVisibleHyperlinks() {
      long next = getNextHyperlinkId();
      setBlockHyperlinkBelowId(next);
   }
   
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
                              _listener.onMouseOrTouchDoubleClick(PromptlyPanel.this, _clientX, _clientY);
                              
                              if (isRestoreFocusToCommandLineOnDoubleClickEvent()) {
                                 focusOnCommandLine();
                              }
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
               boolean focusOnCommandLine = _listener.onSingleClickPanelInCommandMode(PromptlyPanel.this, x3, y3);
               
               if (focusOnCommandLine) {
                  focusOnCommandLine();
               }
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

	
	boolean _promptModeIsText = true;
	
	public void setPromptModeTextMode(boolean isText) {
	   if (_promptModeIsText != isText) {
         _promptZone1.getElement().setAttribute("style", isText ? "" : "display:none;");
         _promptZone2.getElement().setAttribute("style",  isText ? "display:none;" : "");
      }
      _promptModeIsText = isText;
	}
	
	public boolean isPromptModeText() {
	   return _promptModeIsText;
	}
	
	String _promptImageText = "";
	String _promptImageStyle = "";
	
	public void setPromptImage(String embeddedImageText, String style) {
	   _promptImageText = embeddedImageText == null ? "" : embeddedImageText;
	   _promptImageStyle = style;
	   
      _zone2Image.getElement().setAttribute("src",   _promptImageText);
      _zone2Image.getElement().setAttribute("style", _promptImageStyle);
	}
	

   protected final void initCommandLineTextEntryTextBox() {
		_commandLineWrapper = new FlowPanel();
		_commandLineWrapper.getElement().setAttribute("style", "display: flex;");
		_promptZone = new FlowPanel(DivElement.TAG);
		_promptZone1 = new FlowPanel(DivElement.TAG);
		_promptZone2 = new FlowPanel(DivElement.TAG);
		
		{
   		_zone2Image = new FlowPanel(ImageElement.TAG);
   		_zone2Image.getElement().setAttribute("src",   "");
   		_zone2Image.getElement().setAttribute("style", "display:none;");
   		_promptZone2.add(_zone2Image);
		}
		
		_promptChar = new FlowPanel(PreElement.TAG); // Allows the prompt to end in spaces
		String caretStyleVisible = getPromptStyleVisible();
		setPromptCharVisible(caretStyleVisible);
		_promptChar.getElement().setInnerText(">");
		_promptZone1.add(_promptChar);
		_promptZone.add(_promptZone1);
		_promptZone.add(_promptZone2);
		_commandLineWrapper.add(_promptZone);
		_commandLineTextBox = new TextBox() {
			@Override
			protected void onAttach() {
				super.onAttach();
				if (isInitiallyFocusOnTextBox()) {
					focusOnCommandLine();
				}
			}
		};
		// Configurable ? 
		
      _commandLineTextBox.getElement().setAttribute("autocomplete","off");
      _commandLineTextBox.getElement().setAttribute("autocorrect","off");
      _commandLineTextBox.getElement().setAttribute("autocapitalize","off");
      _commandLineTextBox.getElement().setAttribute("spellcheck","false");
      _commandLineTextBox.getElement().setAttribute("type", "email");
		
		String textboxStyle = getDefaultCommandLineTextboxStyle();
		
		setCommandLineTextboxStyle(textboxStyle);
		
		final KeyDownHandler keyDownHandler = new KeyDownHandler() {
			
			@Override
			public void onKeyDown(KeyDownEvent event) {
			   
			   int eventKeyCode = event.getNativeKeyCode();

			   if (! (KeyCodes.KEY_CTRL == eventKeyCode ||
			         eventKeyCode == KeyCodes.KEY_WIN_IME ||
			         eventKeyCode == KeyCodes.KEY_MAC_FF_META ||
			         eventKeyCode == 182 /* vol up */ ||
			         eventKeyCode == 183  /* vol down */ ||
			         KeyCodes.KEY_SHIFT == eventKeyCode ||
			         KeyCodes.KEY_ALT == eventKeyCode ||
			         (eventKeyCode >= KeyCodes.KEY_F1 && eventKeyCode <= KeyCodes.KEY_F12)) 
			    ) {
               _listener.onAnyNonFunctionKeyPressedInAnyMode();
            }
			   
			   if (_collectKeyEventsWhenInNonCommandLineMode /* Collection of events can be halted */) {
			      if ( isAlwaysPropogateZoom(event) ) {
			      // Optionally ignore zoom events (but propogate), this is configurable
			      } else if ((isCaptureCtrlNumerics() && event.isControlKeyDown()) && (eventKeyCode >= KeyCodes.KEY_ZERO && eventKeyCode <= KeyCodes.KEY_NINE)) {
			         // NOTE :: If always propogating zoon, then ctrl+0 is not handled in this block
			         _listener.onControlNumberPressedInAllModes(PromptlyPanel.this, eventKeyCode - KeyCodes.KEY_ZERO);
                  event.preventDefault();
                  event.stopPropagation();
			      } else if (eventKeyCode == KeyCodes.KEY_C && event.isControlKeyDown()) {
   			      _listener.onControlCPressedInAllModes(PromptlyPanel.this);
   			      // We do not stop propogation (by default) as ctrl+c is handy for copy paste
   			      // May make this configurable later.
   			      //event.stopPropagation();
   			   } else {
      				if (_isCommandLineMode) {
                     int nativeKeyCode = eventKeyCode;
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
      					int nativeKeyCode = eventKeyCode;
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
			      if ( isAlwaysPropogateZoom(event) ) {
			         // Do not stop propogation
               } else if ((isCaptureCtrlNumerics() && event.isControlKeyDown()) && (eventKeyCode >= KeyCodes.KEY_ZERO && eventKeyCode <= KeyCodes.KEY_NINE)) {
                  // NOTE :: If always propogating zoon, then ctrl+0 is not handled in this block
                  _listener.onControlNumberPressedInAllModes(PromptlyPanel.this, eventKeyCode - KeyCodes.KEY_ZERO);
                  event.preventDefault();
                  event.stopPropagation();
			      } else {
                  event.preventDefault();
                  event.stopPropagation();
			      }
			   }
			}


		};
		
		_commandLineTextBox.addKeyDownHandler(keyDownHandler);
		_commandLineWrapper.addHandler(keyDownHandler, KeyDownEvent.getType());
		_commandLineWrapper.add(_commandLineTextBox);

	}


   /**
    * Usage: PromptlyPanel.setPromptCharVisible(PromptlyPanel.DEFAULT_CARET_STYLE_VISIBLE);
    * @param promptCharVisible
    */
   private void setPromptCharVisible(String promptCharVisible) {
      if (promptCharVisible != null) {
		   _promptChar.getElement().setAttribute("style", promptCharVisible);
		}
      _promptChar.getElement().setClassName(null);
   }
   
   
   
   String _additionalPromptCharStyles = null;
   
   public void setAdditionalStylesForPromptChar(String additionalPromptCharStyles) {
      _additionalPromptCharStyles = additionalPromptCharStyles;
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
	
   public final ItemHandle appendPre (String paragraphText, String additionalStyle) {
      FlowPanel tag = null;
      if (paragraphText.length() == 0) {
         tag = new FlowPanel(BRElement.TAG);
      } else {
         tag = new FlowPanel(PreElement.TAG);
         if (getPreBlockClassName() != null) {
            tag.getElement().setAttribute("class", getPreBlockClassName());
         }
         
         if (additionalStyle != null) {
            tag.getElement().setAttribute("style",additionalStyle);
         }
         tag.getElement().setInnerText(paragraphText);
      }
      appendAndScrollOrFocusAsAppropriate(tag);
      return new ItemHandle(this, tag);
   }
	
	
	public final ItemHandle appendEmbeddedImage(
      HAlignment imageAlignment,
      String embeddedImageText,
      String embeddedImageStyle,
      String description
	) {
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
	
	public final void appendTiledLineImage(String srcImageText, String additionalCssStyle) {
	   FlowPanel outer = new FlowPanel(DivElement.TAG);
	   outer.getElement().setAttribute("style", additionalCssStyle + "line-height:100%;font-size:1px;width:100%;align:'center';background-image:url('"+srcImageText+"');"); //margin-bottom:6px;
	   FlowPanel inner = new FlowPanel(ImageElement.TAG);
      inner.getElement().setAttribute("src",   srcImageText);
      inner.getElement().setAttribute("style", "visibility: hidden;");
      outer.add(inner);
      appendAndScrollOrFocusAsAppropriate(outer);
	}
	
	/**
	 * 
	 * @param styledBlock A non null styled paragraph instance
	 * @param withFormatting If false, then styles will be ignored.
	 */
   public final void append(StyledBlock styledBlock, boolean withFormatting) {
      FlowPanel outerWidget = new FlowPanel(ParagraphElement.TAG /* <p> tag */ );
      styledBlock.toGwtWidget(this, outerWidget, withFormatting, null /* outer style override */);
      appendAndScrollOrFocusAsAppropriate(outerWidget);
   }
   

   public final void appendPre(StyledBlock styledBlock, boolean withFormatting, String additionalStyle) {
      FlowPanel outerWidget = new FlowPanel(PreElement.TAG );
      styledBlock.toGwtWidget(this, outerWidget, withFormatting, getPreBlockClassName());
      
      if (additionalStyle != null) {
         outerWidget.getElement().setAttribute("style",additionalStyle);
      }
      
      appendAndScrollOrFocusAsAppropriate(outerWidget);
   }
   
   
   public final void mirror(StyledBlock styledBlock, boolean withFormatting) {
      FlowPanel gwtWidget = null;

      if (isPromptModeText() || _promptImageText == null || _promptImageText.length() == 0) {
         FlowPanel commandLineMirrorPart = new FlowPanel(ParagraphElement.TAG /* <p> tag */ );
         styledBlock.toGwtWidget(this, commandLineMirrorPart, withFormatting, null /* outer style override */);
         
         gwtWidget = commandLineMirrorPart;
      } else {
         gwtWidget = new FlowPanel(ParagraphElement.TAG);
         gwtWidget.getElement().setAttribute("style", "align-self:center;display:flex;");
         
         {
            FlowPanel imagePart = new FlowPanel(ImageElement.TAG);
            imagePart.getElement().setAttribute("src",   _promptImageText);
            imagePart.getElement().setAttribute("style", _promptImageStyle);
            gwtWidget.add(imagePart);
         }
         
         // Superspan
         {
            FlowPanel ss = new FlowPanel(SpanElement.TAG);
            ss.getElement().setAttribute("style", "align-self:center;");
            styledBlock.toGwtWidget(this, ss, withFormatting, null /* outer style override */);
            gwtWidget.add(ss);
         }
         //styledBlock.toGwtWidget(this, gwtWidget, withFormatting, null /* outer style override */);
      }
      
      if (gwtWidget != null) {
         appendAndScrollOrFocusAsAppropriate(gwtWidget);
      }
      
   }
   


   private final void appendAndScrollOrFocusAsAppropriate(FlowPanel tag) {
      int numWidgets = _mainTextFlowDiv.getWidgetCount();
      if (numWidgets == 0) {
         _mainTextFlowDiv.add(tag);
      } else {
         _mainTextFlowDiv.insert(tag, _mainTextFlowDiv.getWidgetCount()-1);
      }
	   
      if (_isCommandLineMode) {
         focusOnCommandLine();
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
         _promptChar.getElement().setAttribute("style", additionalStyle);
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
			focusOnCommandLine();
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
			   
			   if (commandLineMode) {
			      
			      
			      if (_isTurnCaretIntoBackgroundColorWhenNotInCommandMode) {
   			      String existingStyle = _commandLineTextBox.getElement().getAttribute("style");
   			      if (existingStyle != null && _blankCaretCss != null && _blankCaretCss.length() > 0) {
   			         existingStyle = existingStyle.replace(_blankCaretCss, "");
   			         _commandLineTextBox.getElement().setAttribute("style", existingStyle);
   			      }
			      }
			   } else {
			      
			      if (_isTurnCaretIntoBackgroundColorWhenNotInCommandMode) {
   			      String existingStyle = _commandLineTextBox.getElement().getAttribute("style");
                  if (existingStyle != null && _blankCaretCss != null && _blankCaretCss.length() > 0) {
                     if ( !existingStyle.contains(_blankCaretCss)) {
                        _commandLineTextBox.getElement().setAttribute("style", existingStyle + _blankCaretCss);
                     }
                  }
			      }
               
               if (isCleardownCommandLineTextWhenSwitchingToNonCommandMode()) {
                  setCommandLineText("");
               }
			   }
			   showPromptChar(commandLineMode);
			   if (commandLineMode != isCommandLineMode()) {
   				boolean refocusOnCommandLine = (!_isCommandLineMode) && commandLineMode;
   				_isCommandLineMode = commandLineMode;
   				if (refocusOnCommandLine) {
   				   focusOnCommandLine();
   				}
			   }
			   _collectKeyEventsWhenInNonCommandLineMode   = collectKeyEventsWhenInNonCommandLineMode;
			   _collectMouseEventsWhenInNonCommandLineMode = collectMouseEventsWhenInNonCommandLineMode;
			}
		};
		Scheduler.get().scheduleDeferred(cmd);
	}
	
	boolean _isShowPromptChar = true;
   private String _blankCaretCss = null;
   private boolean _hidePrompt;
	
	public boolean isShowPromptChar() {
      return _isShowPromptChar;
   }
	
	
	public final void showPromptChar(boolean showPromptChar) {
	   _isShowPromptChar = showPromptChar;
	   if (showPromptChar) {
	        String caretStyleVisible = getPromptStyleVisible();
	         if (caretStyleVisible != null) {
	            _promptChar.getElement().setAttribute("style", caretStyleVisible + (_additionalPromptCharStyles == null ? "" : _additionalPromptCharStyles));
	         } else {
	            _promptChar.getElement().removeAttribute("style");
	         }
	   } else {
         String promptCharStyleInvisible = getPromptStyleInvisible();
         if (promptCharStyleInvisible != null) {
            _promptChar.getElement().setAttribute("style", promptCharStyleInvisible + (_additionalPromptCharStyles == null ? "" : _additionalPromptCharStyles));
         } else {
            _promptChar.getElement().removeAttribute("style");
         }
	   }
	}

	public final void focusOnCommandLine() {
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
   
   public final void setOuterPanelStyle(String overrideString, String blankCaretCss) {
      if (overrideString != null) {
         _outerPanel.getElement().setAttribute("style", overrideString);
      }
      _blankCaretCss = blankCaretCss;
   }   
   public final void setConsoleClass(String overrideString) {
      if (overrideString != null) {
         _mainTextFlowDiv.getElement().setAttribute("class", overrideString);
      }
   }
   
   public final void setPromptChar(String string) {
      _promptChar.getElement().setInnerText(string);
   }
   
   public final Panel getOuterPanel() {
      return _outerPanel;
   }
   
   /**
    * Override this to be able to change contents of console without refocusing on the textbox every time
    * @return Return a false value here to be able to change contents of console without refocusing on the textbox every time. If this returns a false value then a call to {@link #focusOnCommandLine()} does nothing. 
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
   
   public String getPromptStyleVisible() {
      return DEFAULT_PROMPT_STYLE_VISIBLE;
   }
   
   public String getPromptStyleInvisible() {
      return DEFAULT_PROMPT_STYLE_INVISIBLE;
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
      String promptChar = _promptChar.getElement().getInnerText();
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
   
   public boolean isRestoreFocusToCommandLineOnDoubleClickEvent() {
      return true;
   }
   
   public boolean isNeverHandleAndAlwaysPropagateZoomHotkeys() {
      return false;
   }

   
   public boolean isCaptureCtrlNumerics() {
      return false;
   }

   private boolean isAlwaysPropogateZoom(KeyDownEvent event) {
      return isNeverHandleAndAlwaysPropagateZoomHotkeys() && event.isControlKeyDown() && (event.getNativeKeyCode() == KeyCodes.KEY_NUM_PLUS || event.getNativeKeyCode() == KeyCodes.KEY_NUM_MINUS || event.getNativeKeyCode() == KeyCodes.KEY_ZERO);
   }

   public void setHidePrompt(boolean hidePrompt) {
      _hidePrompt = hidePrompt;
      _promptZone.getElement().setAttribute("style", _hidePrompt ? "display:none;align-self:center;" : "align-self:center;");
      
   }



   
}
