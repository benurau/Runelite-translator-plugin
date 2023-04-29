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
import net.runelite.api.events.MenuOpened;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

@PluginDescriptor(
        name = "jap",
        description = "translate npc, items and objects",
        tags = {"actions"}
)
public class TranslatorPlugin extends Plugin
{

    @Inject
    private TranslatorConfig config;
    @Inject
    private Client client;
    @Provides
    TranslatorConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(TranslatorConfig.class);
    }


    public static HashMap<String, String> parse(String filepath) {
        try {
            File myObj = new File(filepath);
            HashMap<String, String> words = new HashMap<String, String>();

            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[] temp = data.split(",");
                if (temp.length > 2) {
                    words.put(temp[0], temp[2]);
                }
            }

            myReader.close();
            return words;

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return null;
        }
    }

    private HashMap<String, String> itemsMap = parse("C:/Users/banu/IdeaProjects/jap/fi_items.txt");;

    private HashMap<String, String> npcMap = parse("C:/Users/banu/IdeaProjects/jap/fi_npc.txt");;

    private HashMap<String, String> objectMap = parse("C:/Users/banu/IdeaProjects/jap/fi_object.txt");


    @Subscribe
    public void onMenuOpened (MenuOpened event)
    {
        System.out.println("here");
        MenuEntry[] menuEntries = client.getMenuEntries();
        MenuEntry[] newMenuEntries = Arrays.copyOf(menuEntries, menuEntries.length);


        for (int idx = 1; idx < newMenuEntries.length; idx++) {
            MenuEntry entry = newMenuEntries[idx];
            System.out.println(entry.getOption());

            if (entry.getWidget() != null && WidgetInfo.TO_GROUP(entry.getWidget().getId()) == WidgetID.EQUIPMENT_GROUP_ID) {
                translateMenuEntrys(this.itemsMap, entry, entry.getWidget().getChild(1).getItemId());
            }
            else if (entry.getItemId() > 0) {
                translateMenuEntrys(this.itemsMap, entry, entry.getItemId());

            }

            else if (entry.getType() == MenuAction.EXAMINE_ITEM_GROUND) {
                translateMenuEntrys(this.itemsMap, entry, entry.getIdentifier());
            }

            else if (entry.getItemId() == -1) {
                if (entry.getPlayer() != null) {
                    System.out.println("player");
                }

                else if (entry.getNpc() != null) {
                    translateMenuEntrys(this.npcMap, entry, entry.getNpc().getId());
                }

                else if (entry.getIdentifier() > 0) {
                    translateMenuEntrys(this.objectMap, entry, entry.getIdentifier());
                }

            }
        }
        client.setMenuEntries(newMenuEntries);


    }

    public void translateMenuEntrys(HashMap<String, String> words, MenuEntry menuEntry, Integer id){


        if (menuEntry.getTarget().length() > 0) {

            String[] subStrings = menuEntry.getTarget().split(">");
            String translated = words.get(id.toString());

            int colStart = subStrings[0].length() + 1;
            String colour = menuEntry.getTarget().substring(0, colStart);


            if (subStrings.length > 2) {
                int combatStart = menuEntry.getTarget().split("<")[1].length() + 1;
                String combat = menuEntry.getTarget().substring(combatStart);

                if (translated != null) {
                    menuEntry.setTarget(colour + translated + combat);
                }

            } else {
                if (translated != null) {
                    menuEntry.setTarget(colour + translated);
                }

            }
        }

    }
}
