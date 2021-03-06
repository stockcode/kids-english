/***************************************************************************************
 * Copyright (c) 2012 Kostas Spyropoulos <inigo.aldana@gmail.com>                       *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.nit.libanki.hooks;

import android.content.SharedPreferences;
import android.util.Log;

import com.nit.vicky.AnkiDroidApp;
import com.nit.libanki.LaTeX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Hooks {
    private Map<String, List<Hook>> hooks;


    public Hooks(SharedPreferences prefs) {
        hooks = new HashMap<String, List<Hook>>();
        // Always-ON hooks
        new FuriganaFilters().install(this);
        new HintFilter().install(this);
        new LaTeX().installHook(this);
        // Preferences activated hooks
        if (prefs.getBoolean("fixHebrewText", false)) {
            HebrewFixFilter.install(this);
        }
        if (prefs.getBoolean("convertFenText", false)) {
            ChessFilter.install(this);
        }
    }


    /**
     * Add a function to hook. Ignore if already on hook.
     * 
     * @param hook The name of the hook.
     * @param func A class implements interface Hook and contains the function to add.
     */
    public void addHook(String hook, Hook func) {
        if (!hooks.containsKey(hook) || hooks.get(hook) == null) {
            hooks.put(hook, new ArrayList<Hook>());
        }
        boolean found = false;
        for (Hook h : hooks.get(hook)) {
            if (func.equals(h)) {
                found = true;
                break;
            }
        }
        if (!found) {
            hooks.get(hook).add(func);
        }
    }


    /**
     * Remove a function if is on hook.
     * 
     * @param hook The name of the hook.
     * @param func A class implements interface Hook and contains the function to remove.
     */
    public void remHook(String hook, Hook func) {
        if (hooks.containsKey(hook) && hooks.get(hook) != null) {
            for (Hook h : hooks.get(hook)) {
                if (func.equals(h)) {
                    hooks.get(hook).remove(h);
                    break;
                }
            }
        }
    }


    /**
     * Run all functions on hook.
     * 
     * @param hook The name of the hook.
     * @param args Variable arguments to be passed to the method runHook of each function on this hook.
     */
    public void runHook(String hook, Object... args) {
        List<Hook> _hook = hooks.get(hook);
        String funcName = "";
        if (_hook != null) {
            try {
                for (Hook func : _hook) {
                    funcName = func.getClass().getCanonicalName();
                    func.runHook(args);
                }
            } catch (Exception e) {
                Log.e(AnkiDroidApp.TAG, "Exception while running hook " +
                        hook + ":" + funcName, e);
                return;
            }
        }
    }


    /**
     * Apply all functions on hook to arg and return the result.
     * 
     * @param hook The name of the hook.
     * @param arg The input to the filter on hook.
     * @param args Variable arguments to be passed to the method runHook of each function on this hook.
     */
    public Object runFilter(String hook, Object arg, Object... args) {
        List<Hook> _hook = hooks.get(hook);
        String funcName = "";
        if (_hook != null) {
            try {
                for (Hook func : _hook) {
                    funcName = func.getClass().getCanonicalName();
                    arg = func.runFilter(arg, args);
                }
            } catch (Exception e) {
                Log.e(AnkiDroidApp.TAG, "Exception while running hook " +
                        hook + ":" + funcName, e);
                return "Error in filter " + hook + ":" + funcName;
            }
        }
        return arg;
    }
}

