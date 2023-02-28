/*
 * Copyright (c) 2017, Aria <aria@ar1as.space>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jap;

import com.google.common.collect.ImmutableSet;
import jdk.internal.org.jline.reader.Widget;
import joptsimple.internal.Strings;
import net.runelite.api.*;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

import javax.inject.Inject;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

class HighlightOverlay extends Overlay
{
    /**
     * Menu types which are on widgets.
     */
    private static final Set<MenuAction> WIDGET_MENU_ACTIONS = ImmutableSet.of(
            MenuAction.WIDGET_TYPE_1,
            MenuAction.WIDGET_TARGET,
            MenuAction.WIDGET_CLOSE,
            MenuAction.WIDGET_TYPE_4,
            MenuAction.WIDGET_TYPE_5,
            MenuAction.WIDGET_CONTINUE,
            MenuAction.ITEM_USE_ON_ITEM,
            MenuAction.WIDGET_USE_ON_ITEM,
            MenuAction.ITEM_FIRST_OPTION,
            MenuAction.ITEM_SECOND_OPTION,
            MenuAction.ITEM_THIRD_OPTION,
            MenuAction.ITEM_FOURTH_OPTION,
            MenuAction.ITEM_FIFTH_OPTION,
            MenuAction.ITEM_USE,
            MenuAction.WIDGET_FIRST_OPTION,
            MenuAction.WIDGET_SECOND_OPTION,
            MenuAction.WIDGET_THIRD_OPTION,
            MenuAction.WIDGET_FOURTH_OPTION,
            MenuAction.WIDGET_FIFTH_OPTION,
            MenuAction.EXAMINE_ITEM,
            MenuAction.WIDGET_TARGET_ON_WIDGET,
            MenuAction.CC_OP_LOW_PRIORITY,
            MenuAction.CC_OP
    );

    private final TooltipManager tooltipManager;
    private final Client client;
    private final HighlightConfig config;

    private final HashMap<String, String> itemsMap;

    private final HashMap<String, String> npcMap;

    private final HashMap<String, String> objectMap;


    @Inject
    HighlightOverlay(Client client, TooltipManager tooltipManager, HighlightConfig config)
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        // additionally allow tooltips above the full screen world map and welcome screen
        drawAfterInterface(WidgetID.FULLSCREEN_CONTAINER_TLI);
        this.client = client;
        this.tooltipManager = tooltipManager;
        this.config = config;
        this.itemsMap = parse("C:/Users/banu/IdeaProjects/jap/fi_items.txt");
        this.npcMap = parse("C:/Users/banu/IdeaProjects/jap/fi_npc.txt");
        this.objectMap = parse("C:/Users/banu/IdeaProjects/jap/fi_npc.txt");
    }

    public static HashMap<String, String> parse(String filepath) {
        try {
            File myObj = new File(filepath);
            HashMap<String, String> words = new HashMap<String, String>();

            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[] temp = data.split(",");
                words.put(temp[0], temp[2]);
            }

            myReader.close();
            return words;

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return null;
        }
    }
    @Override
    public Dimension render(Graphics2D graphics) {
        if (client.isMenuOpen()) {
            onMenuEntryAdded();
        }

        MenuEntry[] menuEntries = client.getMenuEntries();
        int last = menuEntries.length - 1;

        if (last < 0) {
            return null;
        }


        MenuEntry menuEntry = menuEntries[last];
        String target = menuEntry.getTarget();
        String option = menuEntry.getOption();
        MenuAction type = menuEntry.getType();
        Integer itemId = menuEntry.getItemId();





        if (type == MenuAction.RUNELITE_OVERLAY || type == MenuAction.CC_OP_LOW_PRIORITY) {
            // These are always right click only
            return null;
        }

        if (Strings.isNullOrEmpty(option)) {
            return null;
        }

        // Trivial options that don't need to be highlighted, add more as they appear.
        switch (option) {
            case "Walk here":
            case "Cancel":
            case "Continue":
                return null;
            case "Move":
                // Hide overlay on sliding puzzle boxes
                if (target.contains("Sliding piece")) {
                    return null;
                }
        }



        if (WIDGET_MENU_ACTIONS.contains(type)) {
            final int widgetId = menuEntry.getParam1();
            final int groupId = WidgetInfo.TO_GROUP(widgetId);

            if (!config.uiTooltip()) {
                return null;
            }

            if (!config.chatboxTooltip() && groupId == WidgetInfo.CHATBOX.getGroupId()) {
                return null;
            }

            if (config.disableSpellbooktooltip() && groupId == WidgetID.SPELLBOOK_GROUP_ID) {
                return null;
            }
        }

        // If this varc is set, a tooltip will be displayed soon
        int tooltipTimeout = client.getVarcIntValue(VarClientInt.TOOLTIP_TIMEOUT);

        if (tooltipTimeout > client.getGameCycle()) {
            return null;
        }

        // If this varc is set, a tooltip is already being displayed
        int tooltipDisplayed = client.getVarcIntValue(VarClientInt.TOOLTIP_VISIBLE);

        if (tooltipDisplayed == 1) {
            return null;
        }

        if (itemId > 0){
            String translated = itemsMap.get(itemId.toString());
            if (translated != null) {
                tooltipManager.addFront(new Tooltip(option + (Strings.isNullOrEmpty(target) ? " " : " " + translated)));
            }
        }

        if (menuEntry.getItemId() == -1){
            Integer npcId = menuEntry.getNpc().getId();
            String translated = npcMap.get(npcId.toString());

            if (translated != null) {
                tooltipManager.addFront(new Tooltip(option + (Strings.isNullOrEmpty(target) ? " " : " " + translated)));
            }
        }


        return null;
    }

        @Subscribe
        public void onMenuEntryAdded ()
        {
            MenuEntry[] menuEntries = client.getMenuEntries();
            MenuEntry[] newMenuEntries = Arrays.copyOf(menuEntries, menuEntries.length);



            if (newMenuEntries[1].getWidget() != null && WidgetInfo.TO_GROUP(newMenuEntries[1].getWidget().getId()) == WidgetID.EQUIPMENT_GROUP_ID){
                translateMenuEntrys(this.itemsMap, newMenuEntries, newMenuEntries[1].getWidget().getChild(1).getItemId());
            }

            else if (newMenuEntries[1].getItemId() > 0){
                translateMenuEntrys(this.itemsMap, newMenuEntries, newMenuEntries[1].getItemId());
            }

            else if (newMenuEntries[1].getType() == MenuAction.EXAMINE_ITEM_GROUND){
                translateMenuEntrys(this.itemsMap, newMenuEntries, newMenuEntries[1].getIdentifier());
            }

            else if (newMenuEntries[1].getItemId() == -1){
                if (newMenuEntries[1].getPlayer() != null){
                    System.out.println("player");
                }
                else if (newMenuEntries[1].getNpc() != null) {
                    translateMenuEntrys(this.npcMap, newMenuEntries, newMenuEntries[1].getNpc().getId());
                }
                else if(newMenuEntries[1].getIdentifier() > 0){
                    translateMenuEntrys(this.objectMap, newMenuEntries, newMenuEntries[1].getIdentifier());
                }

            }

        }

        public void translateMenuEntrys(HashMap<String, String> words, MenuEntry[] menuEntries, Integer id){



            for (Integer idx = 1; idx < menuEntries.length; idx++) {
                if (menuEntries[idx].getTarget().length() > 0) {
                    int col = menuEntries[idx].getTarget().split(">")[0].length() + 1;
                    String colour = menuEntries[idx].getTarget().substring(0, col);

                    String translated = words.get(id.toString());

                    if (translated != null) {
                        menuEntries[idx].setTarget(colour + translated);
                    }
                }

            }
            client.setMenuEntries(menuEntries);

        }

}
