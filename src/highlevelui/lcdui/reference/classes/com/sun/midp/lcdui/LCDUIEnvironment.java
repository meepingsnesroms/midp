/*
 * Copyright  1990-2006 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version
 * 2 only, as published by the Free Software Foundation. 
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included at /legal/license.txt). 
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA 
 * 
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, CA 95054 or visit www.sun.com if you need additional
 * information or have any questions. 
 */

package com.sun.midp.lcdui;

import com.sun.midp.security.*;
import com.sun.midp.events.EventQueue;

// #ifdef ENABLE_GCI
import com.sun.me.gci.windowsystem.*;
import com.sun.me.gci.windowsystem.event.*;
// #endif ENABLE_GCI

// #ifdef ENABLE_CDC
import com.sun.midp.main.TrustedMIDletIcon;

import com.sun.midp.events.EventTypes;
import com.sun.midp.events.NativeEvent;
import com.sun.midp.configurator.Constants;
// #endif ENABLE_CDC

/**
 * Initialize the LCDUI environment.
 */
public class LCDUIEnvironment {

    /**
     * Creates lcdui event producers/handlers/lisneners.
     * 
     * @param internalSecurityToken
     * @param eventQueue
     * @param isolateId
     * @param foregroundController
     */
    public LCDUIEnvironment(SecurityToken internalSecurityToken,
		EventQueue eventQueue, 
		int isolateId,
		ForegroundController foregroundController) {

        displayEventHandler =
            DisplayEventHandlerFactory.getDisplayEventHandler(
               internalSecurityToken);

        DisplayEventProducer displayEventProducer =
            new DisplayEventProducer(
                eventQueue);

        RepaintEventProducer repaintEventProducer =
            new RepaintEventProducer(
                eventQueue);

        displayContainer = new DisplayContainer(
            internalSecurityToken, isolateId);

        /*
         * Because the display handler is implemented in a javax
         * package it cannot created outside of the package, so
         * we have to get it after the static initializer of display the class
         * has been run and then hook up its objects.
         */
        displayEventHandler.initDisplayEventHandler(
	    displayEventProducer,
            foregroundController,
            repaintEventProducer,
            displayContainer);

        DisplayEventListener displayEventListener = new DisplayEventListener(
            eventQueue,
            displayContainer);

        /* Bad style of type casting, but DisplayEventHandlerImpl
         * implements both DisplayEventHandler & ItemEventConsumer IFs */
        LCDUIEventListener lcduiEventListener = new LCDUIEventListener(
            internalSecurityToken,
            eventQueue,
            (ItemEventConsumer)displayEventHandler);

	// #ifdef ENABLE_GCI
            GCIGraphicsEnvironment gciEnv = 
                GCIGraphicsEnvironment.getInstance();
            GCIEventManager gciEvMgr = gciEnv.getEventManager();
            gciEvMgr.startEventLoop();

            GCIEventListener gciEvLst = new GCIEventListener(eventQueue);
            gciEvMgr.setKeyListener(gciEvLst);
            gciEvMgr.setPointerListener(gciEvLst);
	// #endif ENABLE_GCI
    }

    /**
     * Gets DisplayContainer instance. 
     *
     * @return DisplayContainer
     */
    public DisplayContainer getDisplayContainer(SecurityToken token) {
        token.checkIfPermissionAllowed(Permissions.MIDP);

	return displayContainer;
    }


    /**
     */
    public void initSuiteData(boolean isTrusted) {
        displayEventHandler.initSuiteData(isTrusted);
        
	// #ifdef ENABLE_CDC
        // Needed by the installer
        TrustedMIDletIcon.initClass(
            displayEventHandler.getTrustedMIDletIcon());
        // #endif ENABLE_CDC
    }

    /**   */
    public void closeSuite() {

	// #ifdef ENABLE_CDC
        // shutdown any preempting
	displayEventHandler.donePreempting(null);
	// #endif ENABLE_CDC
    }

    /**
     * Called to register a newly create Display. This method must
     * be called before the other methods can be called.
     *
     * @param displayId ID of the Display
     */
    public void registerDisplay(int displayId) {
	currentDisplayId = displayId;
    }


    /** Stores array of active displays for a MIDlet suite isolate. */
    private DisplayContainer displayContainer;

    /**
     * Provides interface for display preemption, creation and other
     * functionality that can not be publicly added to a javax package.
     */
    private DisplayEventHandler displayEventHandler;

    private int currentDisplayId;

// #ifdef ENABLE_GCI
class GCIEventListener implements 
    GCIKeyEventListener, GCIPointerEventListener {

    /** Preallocate KeyEvent */
    final NativeEvent keyEvent;

    /** Preallocate Pointer Event */
    final NativeEvent pointerEvent;

    /** Cached reference to the MIDP event queue. */
    private EventQueue eventQueue;

    /**
     * The constructor for the GCIEventListener that 
     * gets key and pointer events from GCI,
     * does translate to LCDUI input events and posts them to 
     * LCDUI event queue.
     *
     * @param eventQueue the event queue
     */
    GCIEventListener(EventQueue eventQueue) {
        this.eventQueue = eventQueue;
        keyEvent = new NativeEvent(EventTypes.KEY_EVENT);
        pointerEvent = new NativeEvent(EventTypes.PEN_EVENT);
    }

    /** 
     * Called when GCIKeyEvent is received. It will translate
     * the event into a corresponding MIDP NativeEvent and post
     * it to the event queue.
     *
     * @param event instance of GCIKeyEvent
     */
    public boolean keyEventReceived(GCIKeyEvent event) {

        int type = 0;
        int keyCode = event.getKeyCode();
        int keyChar = event .getKeyChar();

        switch (event.getID()) {
            case GCIKeyEvent.KEY_PRESSED:
                type = EventConstants.PRESSED;
                break;

            case GCIKeyEvent.KEY_RELEASED:
                type = EventConstants.RELEASED;
                break;
   
            default:
                return false;
        }

        boolean foundKeyCode = false;
        for (int i = keyMappingTable.length - 1; i >= 0; i--) {
	    if (keyCode == keyMappingTable[i][1]) {
		keyCode = keyMappingTable[i][0];
                foundKeyCode = true; 
	    }

	}

        if (!foundKeyCode &&
	    (keyCode == GCIKeyEvent.VK_UNDEFINED ||
	     keyChar == GCIKeyEvent.CHAR_UNDEFINED)) {

	    return false;
	}

        keyEvent.intParam1 = type;
        keyEvent.intParam2 = keyCode;
        keyEvent.intParam4 = currentDisplayId;
        eventQueue.post(keyEvent);

        return true;
    }

    /** 
     * Called when GCIPointerEvent is received. It will translate
     * the event into a corresponding MIDP NativeEvent and post
     * it to the event queue.
     *
     * @param event instance of GCIPointerEvent
     */
    public boolean pointerEventReceived(GCIPointerEvent event) {
        int id = event.getID();
        int x = event.getX();
        int y = event.getY();
        int button = event.getButton();
        int modifiers = event.getModifiers();
        long when = System.currentTimeMillis();

        int type = 0;

        switch (event.getID()) {
            case GCIPointerEvent.POINTER_PRESSED:
                type = EventConstants.PRESSED;
                break;

            case GCIPointerEvent.POINTER_RELEASED:
                type = EventConstants.RELEASED;
                break;

            case GCIPointerEvent.POINTER_MOVED:
                type = EventConstants.DRAGGED;
                break;

            default:
                return false;
        }

        pointerEvent.intParam1 = type;
        pointerEvent.intParam2 = event.getX();
        pointerEvent.intParam3 = event.getY();
        pointerEvent.intParam4 = currentDisplayId;
        eventQueue.post(pointerEvent);
	
        return true;
    }

/** Table used to key mapping */
int keyMappingTable[][] = {
    {Constants.KEYCODE_UP,                38},
    {Constants.KEYCODE_DOWN,              40},
    {Constants.KEYCODE_LEFT,              37},
    {Constants.KEYCODE_RIGHT,             39},
    {Constants.KEYCODE_SELECT,            10}
};

}
// #endif ENABLE_GCI

}